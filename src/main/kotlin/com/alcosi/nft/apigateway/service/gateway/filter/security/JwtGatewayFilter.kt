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

import com.alcosi.nft.apigateway.config.WebfluxHeadersHelper
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter.Companion.SECURITY_CLIENT_ATTRIBUTE
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter.Companion.SECURITY_LOG_ORDER
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Mono
import java.security.Principal


abstract class JwtGatewayFilter(
    val securityGatewayFilter: SecurityGatewayFilter,
    protected val authHeaders: List<String>,
    private val order: Int = JWT_LOG_ORDER,
    val jwtHeader: String = AUTHORIZATION_HEADER,
    val securityClientAttribute: String = SECURITY_CLIENT_ATTRIBUTE,
) : MicroserviceGatewayFilter {
    override fun getOrder(): Int {
        return order
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val isOptions = exchange.request.method == HttpMethod.OPTIONS
        if (isOptions) {
            return chain.filter(exchange)
        }
        val isSecurityRequest = securityGatewayFilter.predicate.test(exchange)
        if (!isSecurityRequest) {
            return chain.filter(exchange)
        }
        val token = getHeaderInternal(exchange.request, jwtHeader)?.substring(7)
        val clearedExchange = clearExchange(exchange)
        if (token == null) {
            return chain.filter(clearedExchange)
        } else {
            val exchangeWithClient = mutateExchange(token, clearedExchange, securityClientAttribute)
            val withPrincipal = exchangeWithClient.map { createExchangeWithPrincipal(it) }
            return withPrincipal.flatMap { chain.filter(it) }
        }
    }

    protected fun clearExchange(exchange: ServerWebExchange): ServerWebExchange {
        val rqBuilder = exchange.request.mutate()
        authHeaders.forEach { rqBuilder.header(it, null) }
        val clearRq = rqBuilder.build()
        return exchange.mutate().request(clearRq).build()
    }

    protected open fun createExchangeWithPrincipal(exchangeWithClient: ServerWebExchange): ServerWebExchange {
        return PrincipalWebExchange(exchangeWithClient, securityClientAttribute)
    }

    protected fun getHeaderInternal(request: ServerHttpRequest, authHeader: String): String? {
        val tokenString = WebfluxHeadersHelper.getHeaderOrQuery(request, authHeader)
        return if (tokenString?.startsWith("Bearer") != false) {
            tokenString
        } else "Bearer $tokenString"
    }

    abstract fun mutateExchange(
        jwt: String,
        exchange: ServerWebExchange,
        clientAttribute: String
    ): Mono<ServerWebExchange>

    open class PrincipalWebExchange(
        delegateExchange: ServerWebExchange,
        protected val securityClientAttribute: String
    ) : ServerWebExchangeDecorator(delegateExchange) {
        override fun <T : Principal> getPrincipal(): Mono<T> {
            val attribute = getAttribute<T?>(securityClientAttribute)
            return if (attribute == null) {
                Mono.empty()
            } else {
                Mono.just(attribute as T)
            }
        }

    }

    companion object {
        const val JWT_LOG_ORDER = SECURITY_LOG_ORDER - 10
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}