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

package com.alcosi.nft.apigateway.service.gateway

import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.ethLogin.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The LoginRoutesConfig class is a configuration class that defines routes for login-related endpoints.
 *
 * @param gatewayBasePathProperties The configuration properties for the base path of the gateway.
 */
@Configuration
@EnableConfigurationProperties(GatewayBasePathProperties::class)
@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
class LoginRoutesConfig {
    /**
     * Returns a RouteLocator object for the authorities GET route.
     *
     * @param filtersList The list of MicroserviceGatewayFilters to apply to the route.
     * @param builder The RouteLocatorBuilder used to create the route.
     * @param filter The AuthoritiesGetGatewayFilter used for filtering GET requests and retrieving authorities.
     * @param properties The GatewayBasePathProperties used for configuring the base path of the gateway.
     * @return The created RouteLocator object.
     */
    @Bean
    fun getAuthoritiesRoute(
        filtersList: List<MicroserviceGatewayFilter>,
        builder: RouteLocatorBuilder,
        filter: AuthoritiesGetGatewayFilter,
        properties: GatewayBasePathProperties,
    ): RouteLocator {
        return builder
            .routes()
            .route("authoritiesGet") { r ->
                r.predicate { filter.matches(it.request) }
                    .filters { f ->
                        f.filters(listOf(filter) + filtersList)
                    }
                    .uri(properties.fakeUri)
            }
            .build()
    }

    /**
     * Returns a RouteLocator object for the login GET route.
     *
     * @param filter The LoginGetGatewayFilter used to filter GET requests.
     * @param filtersList The list of MicroserviceGatewayFilters to apply to the route.
     * @param builder The RouteLocatorBuilder used to create the route.
     * @param properties The GatewayBasePathProperties used for configuring the base path of the gateway.
     * @return The created RouteLocator object.
     */
    @Bean
    fun getLoginRoute(
        filter: LoginGetGatewayFilter,
        filtersList: List<MicroserviceGatewayFilter>,
        builder: RouteLocatorBuilder,
        properties: GatewayBasePathProperties,
    ): RouteLocator {
        return builder
            .routes()
            .route("loginGet") { r ->
                r.predicate { filter.matches(it.request) }
                    .filters { f ->
                        f.filters(listOf(filter) + filtersList)
                    }
                    .uri(properties.fakeUri)
            }
            .build()
    }

    /**
     * Creates a RouteLocator object for the post login route.
     *
     * @param filter The LoginPostGatewayFilter used for handling POST requests related to login operations.
     * @param filtersList The list of MicroserviceGatewayFilters to apply to the route.
     * @param builder The RouteLocatorBuilder used to create the route.
     * @param properties The GatewayBasePathProperties used for configuring the base path of the gateway.
     * @return The created RouteLocator object.
     */
    @Bean
    fun postLoginRoute(
        filter: LoginPostGatewayFilter,
        filtersList: List<MicroserviceGatewayFilter>,
        builder: RouteLocatorBuilder,
        properties: GatewayBasePathProperties,
    ): RouteLocator {
        return builder
            .routes()
            .route("loginPost") { r ->
                r.predicate { filter.matches(it.request) }
                    .filters { f ->
                        f.filters(listOf(filter) + filtersList)
                    }
                    .uri(properties.fakeUri)
            }
            .build()
    }

    /**
     * Creates a RouteLocator object for the login PUT route.
     *
     * @param filter The LoginPutGatewayFilter used for handling PUT requests related to login operations.
     * @param filtersList The list of MicroserviceGatewayFilters to apply to the route.
     * @param builder The RouteLocatorBuilder used to create the route.
     * @param properties The GatewayBasePathProperties used for configuring the base path of the gateway.
     * @return The created RouteLocator object.
     */
    @Bean
    fun putLoginRoute(
        filter: LoginPutGatewayFilter,
        filtersList: List<MicroserviceGatewayFilter>,
        builder: RouteLocatorBuilder,
        properties: GatewayBasePathProperties,
    ): RouteLocator {
        return builder
            .routes()
            .route("loginPut") { r ->
                r.predicate { filter.matches(it.request) }
                    .filters { f ->
                        f.filters(listOf(filter) + filtersList)
                    }
                    .uri(properties.fakeUri)
            }
            .build()
    }

    /**
     * Creates a RouteLocator object for the put bound route.
     *
     * @param filter The AuthBoundWalletsPutGatewayFilter used for handling PUT requests to authenticate and bind wallets.
     * @param filtersList The list of MicroserviceGatewayFilters to apply to the route.
     * @param builder The RouteLocatorBuilder used to create the route.
     * @param properties The GatewayBasePathProperties used for configuring the base path of the gateway.
     * @return The created RouteLocator object.
     */
    @Bean
    @ConditionalOnBean(AuthBoundWalletsPutGatewayFilter::class)
    fun putBoundRoute(
        filter: AuthBoundWalletsPutGatewayFilter,
        filtersList: List<MicroserviceGatewayFilter>,
        builder: RouteLocatorBuilder,
        properties: GatewayBasePathProperties,
    ): RouteLocator {
        return builder
            .routes()
            .route("boundPut") { r ->
                r.predicate { filter.matches(it.request) }
                    .filters { f ->
                        f.filters(listOf(filter) + filtersList)
                    }
                    .uri(properties.fakeUri)
            }
            .build()
    }
}
