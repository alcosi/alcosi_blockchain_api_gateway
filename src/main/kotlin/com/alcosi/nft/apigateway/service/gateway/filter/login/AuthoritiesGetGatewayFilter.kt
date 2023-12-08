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

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.dto.ProfileAuthorities
import com.alcosi.nft.apigateway.auth.dto.SecurityClient
import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SECURITY_CLIENT_ATTRIBUTE
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

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val client = exchange.getAttribute<SecurityClient?>(SECURITY_CLIENT_ATTRIBUTE) ?: throw ApiSecurityException(
            4017,
            "Not authorised"
        )
        val authorities = ProfileAuthorities(
            client.authorities.map { com.alcosi.nft.apigateway.auth.dto.ProfileAuthorities.AUTHORITIES.valueOf(it.name) })
        return writer.writeJson(exchange.response, authorities)
    }
    override fun internal(wallet: String, exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Any> {
        TODO("Not yet implemented")
    }

}