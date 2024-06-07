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

import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.openapi.OpenApiDocGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.openapi.SwaggerApiGatewayFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

/**
 * GatewayOpenApiRoutesConfig is a configuration class that defines the routes for Swagger and OpenAPI in a gateway.
 *
 * @property gatewayBasePathProperties The GatewayBasePathProperties used for configuring the base path of the gateway.
 */
@Configuration
@EnableConfigurationProperties(GatewayBasePathProperties::class)
@ConditionalOnProperty(prefix = "opendoc", name = ["disabled"], matchIfMissing = true, havingValue = "false")
class GatewayOpenApiRoutesConfig {
    /**
     * Returns the swagger API route for a gateway.
     *
     * @param resourceLoader The ResourceLoader used for loading resources.
     * @param filter The SwaggerApiGatewayFilter used for filtering requests and handling Swagger API requests.
     * @param properties The GatewayBasePathProperties used for configuring the base path of the gateway.
     * @param builder The RouteLocatorBuilder used for building the route locator.
     * @return The RouteLocator representing the swagger API route.
     */
    @Bean
    fun getSwaggerApiRoute(
        resourceLoader: ResourceLoader,
        filter: SwaggerApiGatewayFilter,
        properties: GatewayBasePathProperties,
        builder: RouteLocatorBuilder,
    ): RouteLocator {
        return builder
            .routes()
            .route("swagger") { r ->
                r.predicate { filter.matches(it.request) }
                    .filters { f ->
                        f.filters(listOf(filter))
                    }
                    .uri(properties.fakeUri)
            }
            .build()
    }

    /**
     * Returns the OpenAPI route for the gateway.
     *
     * @param filter The OpenApiDocGatewayFilter used for filtering requests and handling OpenAPI requests.
     * @param resourceLoader The ResourceLoader used for loading resources.
     * @param properties The GatewayBasePathProperties used for configuring the base path of the gateway.
     * @param writer The GatewayFilterResponseWriter used for writing the filter response.
     * @param builder The RouteLocatorBuilder used for building the route locator.
     * @return The RouteLocator representing the OpenAPI route.
     */
    @Bean
    fun getOpenApiRoute(
        filter: OpenApiDocGatewayFilter,
        resourceLoader: ResourceLoader,
        properties: GatewayBasePathProperties,
        writer: GatewayFilterResponseWriter,
        builder: RouteLocatorBuilder,
    ): RouteLocator {
        return builder
            .routes()
            .route("openApi") { r ->
                r.predicate { filter.matches(it.request) }
                    .filters { f ->
                        f.filters(listOf(filter))
                    }
                    .uri(properties.fakeUri)
            }
            .build()
    }
}
