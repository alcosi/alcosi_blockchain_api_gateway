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
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
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

/**
 * JwtGatewayFilter is an abstract class that implements the MicroserviceGatewayFilter interface.
 * It provides functionality for processing JWT authentication in a gateway filter.
 *
 * @property securityGatewayFilter The SecurityGatewayFilter instance used for security checks.
 * @property authHeaders The list of authentication headers.
 * @property order The order of the filter execution (default is JWT_LOG_ORDER).
 * @property jwtHeader The name of the JWT header (default is AUTHORIZATION_HEADER).
 * @property securityClientAttribute The attribute name used to store the client details in the exchange.
 */
abstract class JwtGatewayFilter(
    val securityGatewayFilter: SecurityGatewayFilter,
    protected val authHeaders: List<String>,
    private val order: Int = JWT_LOG_ORDER,
    val jwtHeader: String = AUTHORIZATION_HEADER,
    val securityClientAttribute: String = SECURITY_CLIENT_ATTRIBUTE,
) : MicroserviceGatewayFilter {
    /**
     * Get the order of the security filter.
     *
     * @return The order of the security filter.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * Filters the incoming request based on certain conditions.
     *
     * @param exchange The [ServerWebExchange] object representing the incoming request.
     * @param chain The [GatewayFilterChain] object to proceed with the filtering.
     * @return A [Mono] object representing the completion of the filtering process.
     */
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
        val tokenHeader = getHeaderInternal(exchange.request, jwtHeader)
        if (tokenHeader!=null&&!tokenHeader.startsWith("Bearer ",true)) {
            throw ApiException(401,"Invalid token header format: $tokenHeader. Should be 'Bearer <token>'")
        }
        val token = tokenHeader?.substring(7)
        val clearedExchange = clearExchange(exchange)
        if (token == null) {
            return chain.filter(clearedExchange)
        } else {
            val exchangeWithClient = mutateExchange(token, clearedExchange, securityClientAttribute)
            val withPrincipal = exchangeWithClient.map { createExchangeWithPrincipal(it) }
            return withPrincipal.flatMap { chain.filter(it) }
        }
    }

    /**
     * Determines whether the given request is a security request.
     *
     * @param exchange The [ServerWebExchange] object representing the incoming request.
     * @return true if the request is a security request, false otherwise.
     */
    protected open fun getIsSecurityRequest(exchange: ServerWebExchange): Boolean {
        val authorities = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] as PathAuthorities?
        return authorities?.haveAuth() == true || securityGatewayFilter.predicate.test(exchange)
    }

    /**
     * Clears the authentication headers from the given ServerWebExchange.
     *
     * @param exchange The ServerWebExchange object representing the incoming request.
     * @return The ServerWebExchange object with the authentication headers cleared.
     */
    protected open fun clearExchange(exchange: ServerWebExchange): ServerWebExchange {
        val rqBuilder = exchange.request.mutate()
        authHeaders.forEach { rqBuilder.header(it, null) }
        val clearRq = rqBuilder.build()
        return exchange.mutate().request(clearRq).build()
    }

    protected open fun createExchangeWithPrincipal(exchangeWithClient: ServerWebExchange): ServerWebExchange {
        return PrincipalWebExchange(exchangeWithClient, securityClientAttribute)
    }

    /**
     * Retrieves the header value based on the given request and authentication header name.
     *
     * @param request The [ServerHttpRequest] object representing the incoming request.
     * @param authHeader The name of the authentication header.
     * @return The value of the authentication header, or null if not present.
     */
    protected open fun getHeaderInternal(
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

    /**
     * Mutates the given ServerWebExchange with the provided JWT token and client attribute.
     *
     * @param jwt The JWT token.
     * @param exchange The ServerWebExchange object representing the incoming request.
     * @param clientAttribute The client attribute.
     * @return A Mono object representing the completion of the mutation process.
     */
    abstract fun mutateExchange(
        jwt: String,
        exchange: ServerWebExchange,
        clientAttribute: String,
    ): Mono<ServerWebExchange>

    /**
     * Represents a decorator class for [ServerWebExchange] with additional functionality for retrieving the principal.
     *
     * @property delegateExchange The delegate [ServerWebExchange] object.
     * @property securityClientAttribute The name of the client attribute.
     */
    open class PrincipalWebExchange(
        delegateExchange: ServerWebExchange,
        protected val securityClientAttribute: String,
    ) : ServerWebExchangeDecorator(delegateExchange) {
        /**
         * Retrieves the principal from the server web exchange.
         *
         * @return Returns a Mono that emits the principal, or an empty Mono if the principal is not found.
         * @param T The type of principal to retrieve.
         */
        override fun <T : Principal> getPrincipal(): Mono<T> {
            val attribute = getAttribute<T?>(securityClientAttribute)
            return if (attribute == null) {
                Mono.empty()
            } else {
                Mono.just(attribute as T)
            }
        }
    }

    /**
     * The companion object for the JwtGatewayFilter class.
     */
    companion object {
        /**
         * Represents the order of the JWT log in a filtering process.
         * The JWT log order is calculated based on the order of the security log minus 10.
         *
         * @see JwtGatewayFilter
         * @see ValidationGatewayFilter
         * @see LoginAbstractGatewayFilter
         * @see AuthBoundWalletsPutGatewayFilter
         * @see EthJwtGatewayFilter
         * @see Oath2GatewayFilter
         */
        const val JWT_LOG_ORDER = SECURITY_LOG_ORDER - 10

        /**
         * Represents the name of the authorization header used in an HTTP request.
         */
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
