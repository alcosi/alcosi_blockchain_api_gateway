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
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicLong

open class RequestHistoryGatewayFilterRq(
    protected val requestDBService: RequestHistoryDBService,
    private val order: Int = Int.MIN_VALUE,
) : MicroserviceGatewayFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val requestInfo = requestDBService.saveRequest(exchange)
        val serverWebExchange =
            exchange.mutate()
                .response(RequestHistoryResponseDecorator(exchange, requestDBService, requestInfo))
                .build()
        return chain.filter(serverWebExchange).then(
            Mono.just(serverWebExchange.response).flatMap {
                logger.log(Level.WARN, "Response code ${it.statusCode}")
                return@flatMap Mono.just("").then()
            },
        )
    }

    override fun getOrder(): Int {
        return order
    }

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
                        requestHistoryDBService.saveRs(requestInfo, deferredContentLength.get(), statusCode)
                    },
                )
        }
    }
}
