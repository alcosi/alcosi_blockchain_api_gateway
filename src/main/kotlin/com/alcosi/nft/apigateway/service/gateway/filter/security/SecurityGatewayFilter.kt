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

import com.alcosi.lib.security.PrincipalDetails
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.PathAuthorities
import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.predicate.SecurityConfigGatewayPredicate
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * The SecurityGatewayFilter class represents a gateway filter used for security authentication and authorization.
 *
 * @property predicate The security gateway predicate used to determine if the request should go through the filter.
 * @property authoritiesAttributeField The name of the attribute field containing the path authorities.
 * @property securityClientAttributeField The name of the attribute field containing the security client.
 * @property order The order of the filter.
 *
 * @constructor Creates a new instance of the SecurityGatewayFilter class.
 * @param predicate The security gateway predicate.
 * @param authoritiesAttributeField The name of the attribute field containing the path authorities.
 * @param securityClientAttributeField The name of the attribute field containing the security client.
 * @param order The order of the filter.
 */
open class SecurityGatewayFilter(
    val predicate: SecurityConfigGatewayPredicate,
    val authoritiesAttributeField: String,
    val securityClientAttributeField: String = SECURITY_CLIENT_ATTRIBUTE,
    private val order: Int = SECURITY_LOG_ORDER,
) : MicroserviceGatewayFilter {
    /**
     * The `errorPath` variable represents the regular expression pattern used to match error paths.
     *
     * @property errorPath The regular expression pattern for error paths.
     */
    protected open val errorPath = "\\/error".toRegex()

    /**
     * Gets the order of the SecurityGatewayFilter instance.
     *
     * @return The order value.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * Filters the request based on certain conditions.
     *
     * @param exchange The ServerWebExchange object representing the current request.
     * @param chain The GatewayFilterChain object representing the chain of filters to be applied.
     * @return A Mono<Void> that completes the request processing.
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        if (exchange.request.method == HttpMethod.OPTIONS) {
            return chain.filter(exchange)
        }
        if (errorPath.matches(exchange.request.path.toString())) {
            return chain.filter(exchange)
        }
        if (!getIsSecurityRequest(exchange)) {
            return chain.filter(exchange)
        } else {
            return processAuth(exchange, chain)
        }
    }

    /**
     * Determines whether the request requires security.
     *
     * @param exchange The ServerWebExchange object representing the current request.
     * @return true if the request requires security, false otherwise.
     */
    protected open fun getIsSecurityRequest(exchange: ServerWebExchange): Boolean {
        val authorities = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] as PathAuthorities?
        return authorities?.haveAuth() == true || predicate.test(exchange)
    }

    /**
     * Processes the authentication for a given ServerWebExchange and GatewayFilterChain.
     *
     * @param exchange The ServerWebExchange representing the current request.
     * @param chain The GatewayFilterChain representing the chain of filters to be applied.
     * @return A Mono<Void> that completes the request processing.
     * @throws ApiSecurityException if the resource requires authentication, the client is of the wrong profile type,
     * or the client does not have the necessary authorities.
     */
    protected open fun processAuth(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val client = exchange.attributes[securityClientAttributeField]
        val authorities = exchange.attributes[authoritiesAttributeField] as PathAuthorities?
        if (client == null) {
            throw ApiSecurityException(
                4012,
                "This resource requires authentication. Please use Bearer token to access this resource",
            )
        } else if (client !is PrincipalDetails) {
            throw ApiSecurityException(4013, "Wrong profile type! ${client.javaClass}")
        } else if (!checkAllAuthority(authorities, client)) {
            val reqAuthString = authorities?.pathAuthorityList?.joinToString("&&") { "(${it.checkMode}:${it.list.joinToString(",")}})" }
            throw ApiSecurityException(
                4014,
                "You don't have authority to access this resource ($reqAuthString). You authorities (${client.authorities.joinToString(",")})",
            )
        } else {
            return chain.filter(exchange)
        }
    }

    /**
     * Checks whether the client has all the required authorities based on the given PathAuthorities object.
     *
     * @param haveToHave The PathAuthorities object representing the required authorities.
     * @param client The PrincipalDetails object representing the client.
     * @return true if the client has all the required authorities, false otherwise.
     */
    protected open fun checkAllAuthority(
        haveToHave: PathAuthorities?,
        client: PrincipalDetails,
    ): Boolean {
        if (haveToHave == null || haveToHave.noAuth()) {
            return true
        }
        return haveToHave.checkHaveAuthorities(client.authorities)
    }

    /**
     * The Companion object for the SecurityGatewayFilter class.
     *
     * This companion object provides constants that are used within the SecurityGatewayFilter class.
     */
    companion object {
        /**
         * Constant for the security client attribute.
         */
        const val SECURITY_CLIENT_ATTRIBUTE = "SecurityClientAttribute"

        /**
         * The variable SECURITY_LOG_ORDER is a constant integer value representing the order of the security log in a gateway filter chain.
         * It is used to determine the position of the security log filter in the filter chain.
         * A lower value means the filter will be executed earlier in the chain.
         * This variable is utilized in multiple classes for ordering purposes.
         */
        const val SECURITY_LOG_ORDER = 20
    }
}
