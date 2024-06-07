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
import com.alcosi.nft.apigateway.auth.service.RefreshTokenService
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.*

/**
 * The LoginPutGatewayFilter class handles the authentication and login process for PUT requests.
 *
 * @property basePath The base path for the API.
 * @property writer The GatewayFilterResponseWriter instance used for handling response writing.
 * @property prepareHexService The PrepareHexService instance used for preparing wallet addresses.
 * @property refreshTokenService The RefreshTokenService instance used for refreshing JSON Web Tokens (JWTs).
 * @property uriRegex The regular expression for matching the URI.
 * @property loginProcessors The list of LoginRequestProcess instances for processing login requests.
 */
open class LoginPutGatewayFilter(
    basePath: String,
    writer: GatewayFilterResponseWriter,
    prepareHexService: PrepareHexService,
    val refreshTokenService: RefreshTokenService,
    uriRegex: String,
    loginProcessors: List<LoginRequestProcess>,
) : LoginAbstractGatewayFilter(basePath, writer, listOf(HttpMethod.PUT), uriRegex, loginProcessors.filter { it.rqTypes().contains(LoginRequestProcess.RequestType.PUT) }, prepareHexService) {
    /**
     * Internal method for retrieving a new JWT and refresh token for the given wallet.
     *
     * @param wallet The wallet name.
     * @param exchange The ServerWebExchange instance.
     * @param chain The GatewayFilterChain instance.
     * @return A Mono emitting the new JWT and refresh token.
     */
    override fun internal(
        wallet: String,
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Any> {
        val wallet = prepareHexService.prepareAddr(getWallet(exchange.request))
        val bodyMonoJson = writer.readBody(exchange, JsonNode::class.java)
        val token =
            bodyMonoJson.flatMap { bodyJson ->
                val jwt = bodyJson["jwt"]?.asText() ?: exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)!!.split(" ")[1]
                val rt = UUID.fromString(bodyJson["rt"].asText())
                val refreshToken = refreshTokenService.refresh(wallet, jwt, rt)
                refreshToken
            }
        return token as Mono<Any>
    }
}
