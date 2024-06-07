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

package com.alcosi.nft.apigateway.service.requestHistory.filter

import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.requestHistory.RequestHistoryDBService
import io.github.breninsul.namedlimitedvirtualthreadexecutor.service.VirtualTreadExecutor
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

/**
 * The RequestHistoryGatewayFilterSecurity class represents a gateway filter that saves request history and handles security.
 * It implements the MicroserviceGatewayFilter interface.
 *
 * @property requestHistoryDBService The service used to interact with the request history database.
 * @property order The order of the filter.
 * @constructor Creates a new RequestHistoryGatewayFilterSecurity instance with the given requestHistoryDBService and order.
 */
open class RequestHistoryGatewayFilterSecurity(
    protected val requestHistoryDBService: RequestHistoryDBService,
    private val order: Int = SecurityGatewayFilter.SECURITY_LOG_ORDER,
) : MicroserviceGatewayFilter {
    /**
     * Filters the request and saves the authentication information into the request history database.
     *
     * @param exchange The ServerWebExchange object representing the current request and response.
     * @param chain The GatewayFilterChain object representing the chain of gateway filters.
     * @return A Mono that completes the request processing.
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val savedFuture = CompletableFuture.supplyAsync(
            { requestHistoryDBService.saveAuth(exchange) },
            executor
        )
        return Mono.fromFuture(savedFuture.thenApply { chain.filter(exchange) }).flatMap { it }
    }

    /**
     * Returns the order of the filter in which it should be applied during the request processing.
     *
     * @return The order value.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * The Companion class represents the companion object of a class. In
     * Kotlin, the companion object is declared inside the class and can
     * contain properties and methods that are related to the class itself.
     *
     * @property executor The executor used by the class. It is protected and
     *     can be accessed by subclasses.
     */
    companion object {
        /**
         * An open protected property representing the executor for running tasks.
         * By default, it creates a new `VirtualThreadPerTaskExecutor`.
         */
        protected open val executor = VirtualTreadExecutor
    }
}
