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
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.regex.Pattern

/**
 * Class that represents a Gateway Filter for handling PUT requests to authenticate and bind wallets.
 *
 * @param basePath The base path for the filter.
 * @param writer The GatewayFilterResponseWriter used to read and write data from and to the response.
 * @param uriRegex The regular expression used to match the URI pattern.
 * @param hexService The PrepareHexService used for preparing hexadecimal addresses.
 * @param mappingHelper The MappingHelper used for mapping objects.
 * @param boundService The BoundWalletsService used for binding wallets.
 * @param nonceComponent The NonceComponent used for managing nonces.
 * @param checkSignatureService The CheckAuthSignatureService used for checking the authenticity of signatures.
 * @param refreshTokenService The RefreshTokenService used for refreshing tokens.
 * @param attrSecurityClient The attribute name for the security client.
 * @param order The order of the filter.
 */
open class AuthBoundWalletsPutGatewayFilter(
    val basePath: String,
    val writer: GatewayFilterResponseWriter,
    uriRegex: String,
    val hexService: PrepareHexService,
    val mappingHelper: ObjectMapper,
    val boundService: BoundWalletsService,
    val nonceComponent: NonceComponent,
    val checkSignatureService: CheckAuthSignatureService,
    val refreshTokenService: RefreshTokenService,
    val attrSecurityClient: String,
    private val order: Int = JWT_LOG_ORDER + 29,
) : ControllerGatewayFilter {
    /**
     * Represents a regular expression pattern used to match URIs.
     * The pattern is constructed by replacing all forward slashes '/' in the `uriRegex` with the escaped version '\\/'.
     * The resulting pattern is compiled to a `java.util.regex.Pattern` object.
     */
    protected val uriPattern: Pattern = uriRegex.replace("/", "\\/").toPattern()

    /**
     * List of HTTP methods supported by the class.
     */
    protected val methods: List<HttpMethod> = listOf(HttpMethod.PUT)

    /**
     * Represents the response object for the `Add` operation.
     *
     * @property status The status of the operation.
     */
    data class AddRs(val status: String)

    /**
     * Returns the profile ID and wallet address pair extracted from the provided ServerHttpRequest.
     *
     * @param req The ServerHttpRequest object from which to extract the profile ID and wallet address.
     * @return A Pair<Long, String> object representing the profile ID and wallet address pair.
     * @throws IllegalStateException if the URI does not match the expected pattern.
     */
    protected fun getProfileWalletPair(req: ServerHttpRequest): Pair<Long, String> {
        val matcher = uriPattern.matcher(basePath + req.path.toString())
        if (!matcher.matches()) {
            throw IllegalStateException("Mot matches uri ${req.path}/$uriPattern")
        }
        return matcher.group("profileid").toLong() to hexService.prepareAddr(matcher.group("walletsecond"))
    }

    /**
     * Filters the incoming request and applies the necessary authentication and authorization logic.
     *
     * @param exchange The ServerWebExchange object representing the incoming request.
     * @param chain The GatewayFilterChain object representing the filter chain.
     * @return A Mono emitting void.
     */
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

    /**
     * Returns the order of this method.
     *
     * @return An integer representing the order of this method.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * Determines if the provided ServerHttpRequest matches the URI pattern and request method.
     *
     * @param request The ServerHttpRequest object to be matched.
     * @return true if the request matches the URI pattern and request method, false otherwise.
     */
    override fun matches(request: ServerHttpRequest): Boolean {
        val toString = request.path.toString()
        val matches = uriPattern.matcher(toString).matches()
        return matches && (methods.contains(request.method))
    }
}
