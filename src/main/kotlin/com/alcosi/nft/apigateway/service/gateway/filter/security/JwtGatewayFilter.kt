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

package com.alcosi.nft.apigateway.service.gateway.filter.security

import com.alcosi.nft.apigateway.config.WebfluxHeadersHelper
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.PathAuthorities
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

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val isOptions = exchange.request.method == HttpMethod.OPTIONS
        if (isOptions) {
            return chain.filter(exchange)
        }
        val isSecurityRequest = getIsSecurityRequest(exchange)
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

    protected open fun getIsSecurityRequest(exchange: ServerWebExchange): Boolean {
        val authorities = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] as PathAuthorities?
        return authorities?.haveAuth() == true || securityGatewayFilter.predicate.test(exchange)
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

    protected fun getHeaderInternal(
        request: ServerHttpRequest,
        authHeader: String,
    ): String? {
        val tokenString = WebfluxHeadersHelper.getHeaderOrQuery(request, authHeader)
        return if (tokenString?.startsWith("Bearer") != false) {
            tokenString
        } else {
            "Bearer $tokenString"
        }
    }

    abstract fun mutateExchange(
        jwt: String,
        exchange: ServerWebExchange,
        clientAttribute: String,
    ): Mono<ServerWebExchange>

    open class PrincipalWebExchange(
        delegateExchange: ServerWebExchange,
        protected val securityClientAttribute: String,
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
