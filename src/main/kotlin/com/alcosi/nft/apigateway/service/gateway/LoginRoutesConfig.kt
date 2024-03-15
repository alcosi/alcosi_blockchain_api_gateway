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

@Configuration
@EnableConfigurationProperties(GatewayBasePathProperties::class)
@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
class LoginRoutesConfig {
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
