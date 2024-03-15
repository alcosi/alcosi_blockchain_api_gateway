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
import com.alcosi.nft.apigateway.auth.dto.ProfileAuthorities
import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter.Companion.SECURITY_CLIENT_ATTRIBUTE
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

open class AuthoritiesGetGatewayFilter(
    basePath: String,
    writer: GatewayFilterResponseWriter,
    uriRegexString: String,
    prepareHexService: PrepareHexService,
) : LoginAbstractGatewayFilter(basePath, writer, listOf(HttpMethod.GET), uriRegexString, listOf(), prepareHexService) {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val client =
            exchange.getAttribute<EthClient?>(SECURITY_CLIENT_ATTRIBUTE) ?: throw ApiSecurityException(
                4017,
                "Not authorised",
            )
        val authorities = ProfileAuthorities(client.authorities)
        return writer.writeJson(exchange.response, authorities)
    }

    override fun internal(
        wallet: String,
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Any> {
        TODO("Not yet implemented")
    }
}
