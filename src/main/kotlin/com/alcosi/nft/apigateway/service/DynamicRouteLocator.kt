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

package com.alcosi.nft.apigateway.service

import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayBaseContextFilter
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import reactor.core.publisher.Flux
import java.net.URI

/**
 * This class represents a dynamic route locator that implements the RouteLocator interface.
 * It is responsible for generating and caching routes based on the provided configuration.
 *
 * @property proxyRoutes The list of proxy route configurations.
 * @property filtersList The list of microservice gateway filters to apply.
 * @property routesBuilder The RouteLocatorBuilder instance used to create routes.
 * @property routesCache The cached Flux of routes.
 */
open class DynamicRouteLocator(
    val proxyRoutes: List<ProxyRouteConfigDTO>,
    val filtersList: List<MicroserviceGatewayFilter>,
    val routesBuilder: RouteLocatorBuilder,
) : RouteLocator {
    /**
     * Flux variable that represents the cache for routes.
     */
    val routesCache: Flux<Route> = initFlux()

    /**
     * Initializes and configures the Flux of routes based on the provided proxyRoutes.
     * It creates and configures Route objects based on the properties of the ProxyRouteConfigDTO object.
     * The routes are filtered using GatewayBaseContextFilter and sorted based on their order property.
     * The resulting Flux of routes is cached and subscribed to.
     *
     * @return A Flux of Route objects representing the configured routes.
     */
    @OptIn(ExperimentalStdlibApi::class)
    open fun initFlux(): Flux<Route> {
        val routesListBuilder = routesBuilder.routes()
        val mappedProxyRotes =
            proxyRoutes
                .fold(routesListBuilder) { builder, routeConfig ->
                    val predicate = routeConfig.toPredicate()
                    val microserviceUri = URI(routeConfig.microserviceUri)
                    val withBasePathFilter = routeConfig.basePathFilter != false
                    val allFilters =
                        if (withBasePathFilter) {
                            val order = (filtersList.maxOfOrNull { it.order } ?: 0) + 1
                            listOf(GatewayBaseContextFilter(order, microserviceUri)) + filtersList
                        } else {
                            filtersList
                        }
                    val mappedUri =
                        if (withBasePathFilter) {
                            URI(microserviceUri.scheme, microserviceUri.authority, null, null, null)
                        } else {
                            microserviceUri
                        }
                    val configuredRoute =
                        builder.route(routeConfig.name ?: routeConfig.hashCode().toHexString()) { route ->
                            route
                                .order(routeConfig.order ?: 0)
                                .predicate(predicate)
                                .filters { f -> f.filters(allFilters) }
                                .uri(mappedUri)
                        }
                    return@fold configuredRoute
                }.build().routes.sort { o1, o2 -> o1.order.compareTo(o2.order) }.cache()
        mappedProxyRotes.subscribe()
        return mappedProxyRotes
    }

    /**
     * Returns a Flux of Route objects representing the configured routes.
     *
     * @return A Flux of Route objects.
     */
    override fun getRoutes(): Flux<Route> {
        return routesCache
    }
}
