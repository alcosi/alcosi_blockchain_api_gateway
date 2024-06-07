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

/**
 * The LoginPostGatewayFilter class is a subclass of LoginAbstractGatewayFilter.
 * It is responsible for handling POST requests related to login operations.
 * This filter performs various tasks such as checking signatures, saving refresh tokens, and verifying nonces.
 * It delegates the actual login process to a list of LoginRequestProcess objects.
 *
 * @param basePath The base path of the filter.
 * @param writer The GatewayFilterResponseWriter used for reading request bodies and writing responses.
 * @param prepareHexService The PrepareHexService for preparing hex values.
 * @param refreshTokenService The RefreshTokenService for refreshing JSON Web Tokens (JWT).
 * @param nonceComponent The NonceComponent for managing nonces.
 * @param checkSignatureService The CheckAuthSignatureService for checking signatures.
 * @param uriRegexString The URI regex pattern to match against.
 * @param loginProcessors The list of LoginRequestProcess objects for handling login requests.
 */
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
