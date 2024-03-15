/*
 * Copyright (c) 2023 Alcosi Group Ltd. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alcosi.nft.apigateway.service.gateway.filter.ethLogin

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.dto.EthClient
import com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService
import com.alcosi.nft.apigateway.auth.service.NonceComponent
import com.alcosi.nft.apigateway.auth.service.RefreshTokenService
import com.alcosi.nft.apigateway.service.gateway.filter.ControllerGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter.Companion.JWT_LOG_ORDER
import com.alcosi.nft.apigateway.service.multiWallet.BoundWalletsService
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.regex.Pattern

open class AuthBoundWalletsPutGatewayFilter(
    val basePath: String,
    val writer: GatewayFilterResponseWriter,
    uriRegex: String,
    val hexService: PrepareHexService,
    val mappingHelper: MappingHelper,
    val boundService: BoundWalletsService,
    val nonceComponent: NonceComponent,
    val checkSignatureService: CheckAuthSignatureService,
    val refreshTokenService: RefreshTokenService,
    val attrSecurityClient: String,
    private val order: Int = JWT_LOG_ORDER + 29,
) : ControllerGatewayFilter {
    protected val uriPattern: Pattern = uriRegex.replace("/", "\\/").toPattern()
    protected val methods: List<HttpMethod> = listOf(HttpMethod.PUT)

    @JvmRecord
    data class AddRs(val status: String)

    protected fun getProfileWalletPair(req: ServerHttpRequest): Pair<Long, String> {
        val matcher = uriPattern.matcher(basePath + req.path.toString())
        if (!matcher.matches()) {
            throw IllegalStateException("Mot matches uri ${req.path}/$uriPattern")
        }
        return matcher.group("profileid").toLong() to hexService.prepareAddr(matcher.group("walletsecond"))
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val profileAndWalletFromPath = getProfileWalletPair(exchange.request)
        val walletSecond = profileAndWalletFromPath.second

        val securityClient = exchange.attributes[attrSecurityClient] as EthClient
        val bodyJsonMono = writer.readBody(exchange, JsonNode::class.java)
        val check =
            bodyJsonMono.flatMap {
                nonceComponent.getSavedNonce(walletSecond)
                    .map { nonce ->
                        checkSignatureService.check(nonce, it["sign"].asText())
                        return@map nonce
                    }
            }
        val rt =
            check.then(boundService.bound(securityClient, walletSecond))
                .mapNotNull { AddRs(it) }
                .then(refreshTokenService.saveInfo(walletSecond))
        return writer.writeMonoJson(exchange.response, rt)
    }

    override fun getOrder(): Int {
        return order
    }

    override fun matches(request: ServerHttpRequest): Boolean {
        val toString = request.path.toString()
        val matches = uriPattern.matcher(toString).matches()
        return matches && (methods.contains(request.method))
    }
}
