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

open class StripBaseUriFilter(basePath: String, private val order: Int = -50) : MicroserviceGatewayFilter {
    protected val toDelete = basePath.count { it == '/' }
    protected val filter =
        StripPrefixGatewayFilterFactory().apply { c: StripPrefixGatewayFilterFactory.Config ->
            c.parts = toDelete
        }

    override fun filter(
        exchange: ServerWebExchange?,
        chain: GatewayFilterChain?,
    ): Mono<Void> {
        return filter.filter(exchange, chain)
    }

    override fun getOrder(): Int {
        return order
    }
}
