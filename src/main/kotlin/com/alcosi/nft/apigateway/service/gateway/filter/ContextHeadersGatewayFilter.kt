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

import com.alcosi.lib.filters.servlet.HeaderHelper
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter.Companion.SECURITY_LOG_ORDER
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * The `ContextHeadersGatewayFilter` class is responsible for adding context headers to the incoming request before passing it through the filter chain.
 *
 * @param serviceName The name of the microservice.
 * @param environment The environment of the microservice.
 * @param order The order of the filter. Default value is `SECURITY_LOG_ORDER + 10`.
 */
open class ContextHeadersGatewayFilter(
    val serviceName: String,
    val environment: String,
    private val order: Int = SECURITY_LOG_ORDER + 10,
) : MicroserviceGatewayFilter {
    /**
     * This method is responsible for adding context headers to the incoming request before passing it through the filter chain.
     *
     * @param exchange The ServerWebExchange object representing the current exchange.
     * @param chain The GatewayFilterChain object representing the filter chain.
     * @return A Mono<Void> indicating completion of the filtering process.
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val apiKey = (exchange.attributes[PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD] as ProxyRouteConfigDTO?)?.apiKey
        val builder =
            exchange
                .request
                .mutate()
                .header(HeaderHelper.ENV_HEADER, environment)
                .header(HeaderHelper.SERVICE_NAME, serviceName)
        if (apiKey != null) {
            builder.header(HeaderHelper.SERVICE_AUTH_HEADER, "Bearer $apiKey")
        }
        exchange.attributes[HeaderHelper.ENV_HEADER] = environment
        exchange.attributes[HeaderHelper.SERVICE_NAME] = serviceName
        val decorated = exchange.mutate().request(builder.build()).build()
        return chain.filter(decorated)
    }

    /**
     * Returns the order of the `ContextHeadersGatewayFilter` filter.
     *
     * @return The order of the filter.
     */
    override fun getOrder(): Int {
        return order
    }
}
