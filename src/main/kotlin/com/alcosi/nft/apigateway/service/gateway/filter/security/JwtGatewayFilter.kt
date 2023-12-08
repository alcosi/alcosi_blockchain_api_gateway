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

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.auth.dto.SecurityClient
import com.alcosi.nft.apigateway.auth.service.CheckJWTService
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import org.apache.commons.lang3.StringUtils
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Mono
import java.security.Principal

const val JWT_LOG_ORDER = 10
const val SECURITY_CLIENT_ATTRIBUTE = "SecurityClientAttribute"
const val AUTHORIZATION_HEADER = "Authorization"
const val X_CLIENT_WALLET_HEADER = "X-Client-Wallet"
const val X_CLIENT_WALLET_LIST_HEADER = "X-Client-Wallets"
const val X_CLIENT_ID_HEADER = "X-Client-Id"

open class JwtGatewayFilter(
    val securityGatewayFilter:SecurityGatewayFilter,
    protected val checkJWTService: CheckJWTService,
    protected val mappingHelper: MappingHelper,
) :
    MicroserviceGatewayFilter {
    override fun getOrder(): Int {
        return JWT_LOG_ORDER
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val isOptions = exchange.request.method == HttpMethod.OPTIONS
        val isSecurityRequest = securityGatewayFilter.predicate.test(exchange)
        if (isOptions||!isSecurityRequest) {
            return chain.filter(exchange)
        }
        val jwt = getHeaderInternal(exchange.request)?.substring(7)
        val exchangeWithClient = if (jwt != null) {
            val claims = checkJWTService.parse(jwt)
            val currentWallet = claims.get("currentWallet", String::class.java)
            val profileWallets = claims.get("profileWallets",List::class.java) as List<String>
            val profileId = claims.get("profileId", String::class.java)
            exchange.attributes[SECURITY_CLIENT_ATTRIBUTE] = SecurityClient(currentWallet,profileWallets, profileId)
            val withWallet = exchange.request
                .mutate()
                .header(X_CLIENT_WALLET_HEADER, currentWallet)
                .header(X_CLIENT_WALLET_LIST_HEADER,profileWallets.joinToString (","))
                .header(X_CLIENT_ID_HEADER,profileId)
                .build()
            exchange.mutate().request(withWallet).build()
        } else {
            val withNoWallet = exchange.request.mutate().header(X_CLIENT_WALLET_HEADER, null).build()
            exchange.mutate().request(withNoWallet).build()
        }
        return chain.filter(PrincipalWebExchange(exchangeWithClient))
    }

   open class PrincipalWebExchange(
        delegateExchange: ServerWebExchange,
    ) : ServerWebExchangeDecorator(delegateExchange) {
        override fun <T : Principal> getPrincipal(): Mono<T> {
            val attribute = getAttribute<T?>(SECURITY_CLIENT_ATTRIBUTE)
            return if (attribute == null) {
                Mono.empty()
            } else {
                Mono.just(attribute as T)
            }
        }

    }

    protected fun getHeaderInternal(request: ServerHttpRequest): String? {
        val tokenString = request.headers[AUTHORIZATION_HEADER]?.firstOrNull()
        return if (tokenString.isNullOrEmpty()) {
            val parameter = request.queryParams[AUTHORIZATION_HEADER]?.firstOrNull() ?: return null
            if (request.method == HttpMethod.GET &&
                StringUtils.isNotEmpty(parameter) &&
                !parameter.startsWith("Bearer")
            ) {
                "Bearer $parameter"
            } else parameter
        } else tokenString
    }


}