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

package com.alcosi.nft.apigateway.service.gateway.filter.security

import com.alcosi.nft.apigateway.auth.dto.SecurityClient
import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.function.Predicate


open class SecurityGatewayFilter(
    val predicate: Predicate<ServerWebExchange>,
) : MicroserviceGatewayFilter {
    private val requiredAuthority = SecurityClient.AUTHORITY.ALL
    override fun getOrder(): Int {
        return JWT_LOG_ORDER + 10;
    }

    protected val errorPath = "\\/error".toRegex()

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        if (exchange.request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }
        if (errorPath.matches(exchange.request.path.toString())) {
            return chain.filter(exchange)
        }
        val haveToAuth = predicate.test(exchange)
        if (!haveToAuth) {
            return chain.filter(exchange)
        } else {
            val client = exchange.attributes[SECURITY_CLIENT_ATTRIBUTE]
            if (client == null) {
                throw ApiSecurityException(
                    4012,
                    "This resource requires authentication. Please use Bearer JWT token to access this resource"
                )
            } else if (client !is com.alcosi.nft.apigateway.auth.dto.SecurityClient) {
                throw ApiSecurityException(4013, "Wrong profile type! ${client.javaClass}")
            } else if (!client.authorities.contains(requiredAuthority)) {
                throw ApiSecurityException(
                    4014,
                    "You don't have authority to access this resource ($requiredAuthority). You authorities (${
                        client.authorities.joinToString(",")
                    })"
                )
            } else {
                return chain.filter(exchange)
            }
        }
    }


}