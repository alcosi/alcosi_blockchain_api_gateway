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

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.PrefixPathGatewayFilterFactory
import org.springframework.core.Ordered
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.URI

/**
 * This class is an implementation of the `GatewayFilter` interface, which filters requests and responses
 * passing through the gateway based on the base context path of a microservice.
 *
 * @param orderInt The order in which this filter is applied in the filter chain.
 * @param microserviceUri The URI of the microservice to which requests should be proxied.
 */
open class GatewayBaseContextFilter(val orderInt: Int, microserviceUri: URI) : GatewayFilter, Ordered {
    /**
     * The delegate property is of type [GatewayFilter] and represents the filter that will be used
     * to process requests and responses passing through the gateway.
     * It is a protected property, which means it can only be accessed within the class or its subclasses.
     *
     * This property is initialized in the constructor of the [GatewayBaseContextFilter] class.
     * If the `microserviceUri` parameter has a non-blank path, the delegate property is set to a new instance
     * of [PrefixPathGatewayFilterFactory], configured with the prefix as the path of the `microserviceUri`.
     * Otherwise, the delegate property is set to a default [GatewayFilter] that simply passes the request
     * and response through without making any modifications.
     *
     * When the [filter] function is called, the delegate filter is invoked on the provided [ServerWebExchange]
     * and [GatewayFilterChain]. The result of the delegate's filter operation is returned as a [Mono] wrapping
     * a [Void].
     *
     * @see GatewayBaseContextFilter
     * @see GatewayFilter
     * @see PrefixPathGatewayFilterFactory
     */
    protected val delegate: GatewayFilter

    /**
     * This init block checks whether the path of the provided
     * `microserviceUri` is not null and is non-blank. If it matches
     * this condition, it initializes the `delegate` property with a new
     * instance of [PrefixPathGatewayFilterFactory] and sets its `prefix`
     * configuration with the path from the `microserviceUri`. If the path of
     * `microserviceUri` is either null or blank, it initializes the `delegate`
     * property with a default [GatewayFilter] that does not alter requests or
     * responses and merely passes them through.
     */
    init {
        if (microserviceUri.path != null && microserviceUri.path.isNotBlank()) {
            val config = PrefixPathGatewayFilterFactory.Config()
            config.prefix = microserviceUri.path
            delegate = PrefixPathGatewayFilterFactory().apply(config)
        } else {
            delegate = GatewayFilter { exchange, chain -> chain!!.filter(exchange) }
        }
    }

    /**
     * Filters the given server web exchange and delegates to the next filter in the chain.
     *
     * @param exchange The server web exchange to be filtered.
     * @param chain The gateway filter chain.
     *
     * @return A Mono indicating when the filtering process has completed.
     */
    override fun filter(
        exchange: ServerWebExchange?,
        chain: GatewayFilterChain?,
    ): Mono<Void> {
        return delegate.filter(exchange, chain)
    }

    /**
     * Returns the order value of this GatewayBaseContextFilter.
     *
     * The order value determines the position of this filter in the filter chain
     * relative to other filters. Filters with a lower order value are executed
     * before filters with a higher order value. Filters with the same order value
     * may be executed in any order.
     *
     * @return The order value of this GatewayBaseContextFilter.
     */
    override fun getOrder(): Int {
        return orderInt
    }
}
