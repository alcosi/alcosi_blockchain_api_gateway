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

/**
 * This class represents a Validation Gateway Filter that extends the MicroserviceGatewayFilter class. It is responsible for filtering requests based on validation rules.
 *
 * @property validationService The FilterValidationService used for request validation.
 * @property predicate The predicate function used to determine if a request requires validation.
 * @property order The order of the filter in the filter chain.
 */
open class ValidationGatewayFilter(
    val validationService: FilterValidationService,
    val predicate: Predicate<ServerWebExchange>,
    private val order: Int = JWT_LOG_ORDER + 11,
) : MicroserviceGatewayFilter {
    /**
     * Returns the order of the ValidationGatewayFilter.
     *
     * @return The order of the ValidationGatewayFilter.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * Filters the incoming request based on certain conditions and performs validation if required.
     * Returns a Mono object representing the completion of the filtering process.
     *
     * @param exchange The ServerWebExchange object representing the incoming HTTP request.
     * @param chain The GatewayFilterChain object representing the chain of filters to be applied.
     * @return A Mono object representing the completion of the filtering process.
     */
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

    /**
     * Sets the validation header value in the given ServerWebExchange object.
     *
     * @param exchange The ServerWebExchange object in which to set the validation header.
     * @param value The boolean value to set as the validation header value.
     * @return The modified ServerWebExchange object with the updated validation header.
     */
    protected open fun setValidationHeader(
        exchange: ServerWebExchange,
        value: Boolean?,
    ): ServerWebExchange {
        val rqBuilder = exchange.request.mutate()
        rqBuilder.header(RequestValidator.Attributes.VALIDATION_IS_PASSED, value?.toString())
        val modExchange = exchange.mutate().request(rqBuilder.build()).build()
        return modExchange
    }
}
