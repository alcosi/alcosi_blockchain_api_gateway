/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.alcosi.nft.apigateway.service.gateway.filter.login

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.dto.SecurityClient
import com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService
import com.alcosi.nft.apigateway.auth.service.NonceComponent
import com.alcosi.nft.apigateway.auth.service.RefreshTokenService
import com.alcosi.nft.apigateway.service.gateway.filter.ControllerGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.JWT_LOG_ORDER
import com.alcosi.nft.apigateway.service.gateway.filter.security.SECURITY_CLIENT_ATTRIBUTE
import com.alcosi.nft.apigateway.service.multi_wallet.BoundWalletsService
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
    ) : ControllerGatewayFilter {
    protected val uriPattern: Pattern = uriRegex.replace("/", "\\/").toPattern()
    protected val methods:List<HttpMethod> = listOf(HttpMethod.PUT)
    @JvmRecord
    data class AddRs(val status:String) {
    }

    protected fun getProfileWalletPair(req: ServerHttpRequest): Pair<Long, String> {
        val matcher = uriPattern.matcher(basePath + req.path.toString())
        if (!matcher.matches()) {
            throw IllegalStateException("Mot matches uri ${req.path}/${uriPattern}")
        }
        return matcher.group("profileid").toLong() to hexService.prepareAddr(matcher.group("walletsecond"))
    }


    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val profileAndWalletFromPath = getProfileWalletPair(exchange.request)
        val walletSecond = profileAndWalletFromPath.second

        val securityClient = exchange.attributes[SECURITY_CLIENT_ATTRIBUTE] as SecurityClient
        val bodyJsonMono = writer.readBody(exchange, JsonNode::class.java)
        val check =
            bodyJsonMono.flatMap {
                nonceComponent.getSavedNonce(walletSecond)
                    .map { nonce ->
                        checkSignatureService.check(nonce, it["sign"].asText())
                        return@map nonce
                    }
            }
        val rt = check.then(boundService.bound(securityClient, walletSecond))
            .mapNotNull { AddRs(it) }
            .then(  refreshTokenService.saveInfo(walletSecond))
        return writer.writeMonoJson( exchange.response, rt)
    }

    override fun getOrder(): Int {
        return JWT_LOG_ORDER + 29;
    }

    override fun matches(request: ServerHttpRequest): Boolean {
        val toString = request.path.toString()
        val matches = uriPattern.matcher(toString).matches()
        return matches && (methods.contains(request.method))
    }

}