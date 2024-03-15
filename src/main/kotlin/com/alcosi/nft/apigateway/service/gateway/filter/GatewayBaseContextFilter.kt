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

open class GatewayBaseContextFilter(val orderInt: Int, microserviceUri: URI) : GatewayFilter, Ordered {
    protected val delegate: GatewayFilter

    init {
        if (microserviceUri.path != null && microserviceUri.path.isNotBlank()) {
            val config = PrefixPathGatewayFilterFactory.Config()
            config.prefix = microserviceUri.path
            delegate = PrefixPathGatewayFilterFactory().apply(config)
        } else {
            delegate = GatewayFilter { exchange, chain -> chain!!.filter(exchange) }
        }
    }

    override fun filter(
        exchange: ServerWebExchange?,
        chain: GatewayFilterChain?,
    ): Mono<Void> {
        return delegate.filter(exchange, chain)
    }

    override fun getOrder(): Int {
        return orderInt
    }
}
