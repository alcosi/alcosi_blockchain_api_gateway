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
import com.alcosi.nft.apigateway.auth.service.LoginRequestProcess
import com.alcosi.nft.apigateway.auth.service.NonceComponent
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * LoginGetGatewayFilter is a class that extends LoginAbstractGatewayFilter and is responsible for handling GET login requests.
 *
 * @param basePath The base path of the URL.
 * @param writer The GatewayFilterResponseWriter instance used for writing response.
 * @param prepareHexService The PrepareHexService instance used for preparing hexadecimal data.
 * @param nonceComponent The NonceComponent instance used for generating and managing nonces.
 * @param uriRegexString The regular expression to match the URI of the request.
 * @param loginProcessors The list of LoginRequestProcess instances for processing login requests.
 */
open class LoginGetGatewayFilter(
    basePath: String,
    writer: GatewayFilterResponseWriter,
    prepareHexService: PrepareHexService,
    val nonceComponent: NonceComponent,
    uriRegexString: String,
    loginProcessors: List<LoginRequestProcess>,
) : LoginAbstractGatewayFilter(
        basePath,
        writer,
        listOf(HttpMethod.GET),
        uriRegexString,
        loginProcessors.filter { it.rqTypes().contains(LoginRequestProcess.RequestType.GET) },
        prepareHexService,
    ) {
    /**
     * Retrieves a new nonce associated with the specified wallet.
     *
     * @param wallet The wallet for which to retrieve the nonce.
     * @param exchange The ServerWebExchange instance.
     * @param chain The GatewayFilterChain instance.
     * @return A [Mono] emitting the new [ClientNonce].
     */
    override fun internal(
        wallet: String,
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Any> {
        return nonceComponent.getNewNonce(wallet) as Mono<Any>
    }
}
