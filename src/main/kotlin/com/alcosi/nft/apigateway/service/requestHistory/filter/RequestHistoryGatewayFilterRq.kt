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
import com.alcosi.nft.apigateway.service.requestHistory.RequestHistoryDBService
import io.github.breninsul.namedlimitedvirtualthreadexecutor.service.VirtualTreadExecutor
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

/**
 * The `RequestHistoryGatewayFilterRq` class represents a gateway
 * filter for recording and storing request history. It implements the
 * `MicroserviceGatewayFilter` interface.
 *
 * @param requestDBService The instance of `RequestHistoryDBService` used
 *     to save request history.
 * @param order The order of the filter. Default is `Int.MIN_VALUE`.
 */
open class RequestHistoryGatewayFilterRq(
    protected val requestDBService: RequestHistoryDBService,
    private val order: Int = Int.MIN_VALUE,
) : MicroserviceGatewayFilter {
    /**
     * Filters the incoming request and handles the response.
     *
     * @param exchange The ServerWebExchange object representing the incoming
     *     request and response.
     * @param chain The GatewayFilterChain object representing the filter
     *     chain.
     * @return A Mono indicating the completion of the filter process.
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val requestInfoMono = Mono.fromFuture(CompletableFuture.supplyAsync({ requestDBService.saveRequest(exchange) }, executor))
        val serverWebExchangeMono =
            requestInfoMono.map {
                exchange.mutate()
                    .response(RequestHistoryResponseDecorator(exchange, requestDBService, it))
                    .build()
            }
        return serverWebExchangeMono.flatMap { serverWebExchange ->
            chain.filter(serverWebExchange).then(logResponseCode(serverWebExchange))
        }
    }

    /**
     * Logs the HTTP response code of the given ServerWebExchange.
     *
     * @param exchange The ServerWebExchange object representing the incoming request and response.
     * @return A Mono indicating the completion of the logging process.
     */
    protected open fun logResponseCode(exchange: ServerWebExchange): Mono<Void> {
        logger.log(Level.WARN, "Response code ${exchange.response.statusCode}")
        return Mono.just("").then()
    }

    /**
     * Returns the order of the request history.
     *
     * @return The order of the request history.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * RequestHistoryResponseDecorator is a class that decorates the
     * ServerHttpResponse and overrides the writeWith method to save the
     * response history to the database.
     *
     * @property exchange The ServerWebExchange object representing the
     *     incoming request and response.
     * @property requestHistoryDBService The RequestHistoryDBService instance
     *     used for saving the response history.
     * @property requestInfo The HistoryRqInfo object representing the request
     *     history information.
     */
    open class RequestHistoryResponseDecorator(
        protected val exchange: ServerWebExchange,
        protected val requestHistoryDBService: RequestHistoryDBService,
        protected val requestInfo: RequestHistoryDBService.HistoryRqInfo,
    ) : ServerHttpResponseDecorator(exchange.response), Logging {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            val deferredContentLength = AtomicLong(0)
            val countedBody =
                Flux.from(body).map { dataBuffer ->
                    deferredContentLength.addAndGet(dataBuffer.readableByteCount().toLong())
                    dataBuffer
                }
            return super
                .writeWith(
                    countedBody.doOnComplete {
                        executor.execute {
                            requestHistoryDBService.saveRs(requestInfo, deferredContentLength.get(), statusCode)
                        }
                    },
                )
        }
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
