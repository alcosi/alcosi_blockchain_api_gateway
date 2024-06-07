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

package com.alcosi.nft.apigateway.service.gateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * The `StripBaseUriFilter` class is an implementation of the `MicroserviceGatewayFilter` interface.
 * It is used to strip the base URI from the request path.
 *
 * @param basePath The base path used for filtering.
 * @param order The order of the filter in the filter chain. Default value is -50.
 */
open class StripBaseUriFilter(basePath: String, private val order: Int = -50) : MicroserviceGatewayFilter {
    /**
     * The `toDelete` variable is a protected property of type `Int`.
     * It represents the number of occurrences of slashes ('/') in the `basePath`.
     *
     * @see StripBaseUriFilter
     *
     * @property toDelete The number of occurrences of slashes ('/') in the `basePath`.
     */
    protected val toDelete = basePath.count { it == '/' }

    /**
     * The `filter` variable is a protected property of type `StripPrefixGatewayFilterFactory`.
     * It is used to create and configure an instance of `StripPrefixGatewayFilterFactory`.
     *
     * @see StripBaseUriFilter
     * @see StripPrefixGatewayFilterFactory
     *
     * @property filter The instance of `StripPrefixGatewayFilterFactory` used for filtering.
     */
    protected val filter =
        StripPrefixGatewayFilterFactory().apply { c: StripPrefixGatewayFilterFactory.Config ->
            c.parts = toDelete
        }

    /**
     * The `filter` method is used to apply the `StripPrefixGatewayFilterFactory` filter to the `exchange`
     * using the provided `chain` in a microservice gateway.
     *
     * @param exchange The ServerWebExchange representing the current request and response.
     * @param chain The GatewayFilterChain used to apply the filter to the next filters in the filter chain.
     * @return A Mono<Void> representing the completion of the filter operation.
     */
    override fun filter(
        exchange: ServerWebExchange?,
        chain: GatewayFilterChain?,
    ): Mono<Void> {
        return filter.filter(exchange, chain)
    }

    /**
     * Returns the order value of this filter.
     *
     * @return The order value of this filter.
     */
    override fun getOrder(): Int {
        return order
    }
}
