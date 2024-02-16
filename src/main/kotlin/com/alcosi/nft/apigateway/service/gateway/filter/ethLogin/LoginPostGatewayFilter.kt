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

package com.alcosi.nft.apigateway.service.gateway.filter.ethLogin

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService
import com.alcosi.nft.apigateway.auth.service.LoginRequestProcess
import com.alcosi.nft.apigateway.auth.service.NonceComponent
import com.alcosi.nft.apigateway.auth.service.RefreshTokenService
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

open class LoginPostGatewayFilter(
    basePath: String,
    writer: GatewayFilterResponseWriter,
    prepareHexService: PrepareHexService,
    val refreshTokenService: RefreshTokenService,
    val nonceComponent: NonceComponent,
    val checkSignatureService: CheckAuthSignatureService,
    uriRegexString: String,
    loginProcessors: List<LoginRequestProcess>,
) : LoginAbstractGatewayFilter(basePath, writer, listOf(HttpMethod.POST), uriRegexString, loginProcessors.filter { it.rqTypes().contains(LoginRequestProcess.RequestType.POST) }, prepareHexService) {
    override fun internal(
        wallet: String,
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Any> {
        val bodyJsonMono = writer.readBody(exchange, JsonNode::class.java)
        val tokenMono =
            bodyJsonMono
                .flatMap {
                    val check =
                        nonceComponent.getSavedNonce(wallet)
                            .map { nonce ->
                                checkSignatureService.check(nonce, it["sign"].asText())
                                return@map nonce
                            }
                    return@flatMap check.then(refreshTokenService.saveInfo(wallet))
                }
        return tokenMono as Mono<Any>
    }
}