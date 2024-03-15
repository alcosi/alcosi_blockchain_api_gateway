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

open class SecurityGatewayFilter(
    val predicate: SecurityConfigGatewayPredicate,
    val authoritiesAttributeField: String,
    val securityClientAttributeField: String = SECURITY_CLIENT_ATTRIBUTE,
    private val order: Int = SECURITY_LOG_ORDER,
) : MicroserviceGatewayFilter {
    protected open val errorPath = "\\/error".toRegex()

    override fun getOrder(): Int {
        return order
    }

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

    protected open fun getIsSecurityRequest(exchange: ServerWebExchange): Boolean {
        val authorities = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] as PathAuthorities?
        return authorities?.haveAuth() == true || predicate.test(exchange)
    }

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

    protected open fun checkAllAuthority(
        haveToHave: PathAuthorities?,
        client: PrincipalDetails,
    ): Boolean {
        if (haveToHave == null || haveToHave.noAuth()) {
            return true
        }
        return haveToHave.checkHaveAuthorities(client.authorities)
    }

    companion object {
        const val SECURITY_CLIENT_ATTRIBUTE = "SecurityClientAttribute"
        const val SECURITY_LOG_ORDER = 20
    }
}
