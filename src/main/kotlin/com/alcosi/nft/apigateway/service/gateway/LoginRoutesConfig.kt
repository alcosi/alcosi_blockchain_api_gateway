/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
