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

import com.alcosi.nft.apigateway.service.error.exceptions.ApiValidationException
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter.Companion.JWT_LOG_ORDER
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.FilterValidationService
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.RequestValidator
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.function.Predicate

open class ValidationGatewayFilter(
    val validationService: FilterValidationService,
    val predicate: Predicate<ServerWebExchange>,
    private val order: Int = JWT_LOG_ORDER + 11,
) : MicroserviceGatewayFilter {
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
        val clearExchange = setValidationHeader(exchange, null)
        val haveToAuth = predicate.test(clearExchange)
        return if (!haveToAuth) {
            chain.filter(clearExchange)
        } else {
            validationService.check(clearExchange)
                .flatMap {
                    clearExchange.attributes[RequestValidator.Attributes.VALIDATION_IS_PASSED] = it.success
                    if (!it.success) {
                        Mono.error(ApiValidationException(it.errorDescription))
                    } else {
                        val modExchange = setValidationHeader(clearExchange, it.success)
                        chain.filter(modExchange)
                    }
                }
        }
    }

    private fun setValidationHeader(
        exchange: ServerWebExchange,
        value: Boolean?,
    ): ServerWebExchange {
        val rqBuilder = exchange.request.mutate()
        rqBuilder.header(RequestValidator.Attributes.VALIDATION_IS_PASSED, value?.toString())
        val modExchange = exchange.mutate().request(rqBuilder.build()).build()
        return modExchange
    }
}
