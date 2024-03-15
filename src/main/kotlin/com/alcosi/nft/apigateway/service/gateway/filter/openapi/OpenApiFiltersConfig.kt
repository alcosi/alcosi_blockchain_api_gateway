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

package com.alcosi.nft.apigateway.service.gateway.filter.openapi

import com.alcosi.nft.apigateway.service.gateway.GatewayBasePathProperties
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
@EnableConfigurationProperties(OpenApiProperties::class, GatewayBasePathProperties::class)
@ConditionalOnProperty(prefix = "opendoc", name = ["disabled"], matchIfMissing = true, havingValue = "false")
class OpenApiFiltersConfig {
    @Bean
    @ConditionalOnMissingBean(SwaggerApiGatewayFilter::class)
    fun getSwaggerApiFilter(
        openApiProperties: OpenApiProperties,
        gatewayBasePathProperties: GatewayBasePathProperties,
        resourceLoader: ResourceLoader,
        writer: GatewayFilterResponseWriter,
    ): SwaggerApiGatewayFilter {
        return SwaggerApiGatewayFilter(
            writer,
            resourceLoader,
            "${gatewayBasePathProperties.path}${openApiProperties.swaggerUri}",
            openApiProperties.swaggerFilePath,
            "${gatewayBasePathProperties.path}${openApiProperties.openApiFileUri}",
        )
    }

    @Bean
    @ConditionalOnMissingBean(OpenApiDocGatewayFilter::class)
    fun getOpenApiFilter(
        openApiProperties: OpenApiProperties,
        gatewayBasePathProperties: GatewayBasePathProperties,
        @Value("\${gateway.base.path:/api}")
        basePath: String,
        resourceLoader: ResourceLoader,
        writer: GatewayFilterResponseWriter,
        builder: RouteLocatorBuilder,
    ): OpenApiDocGatewayFilter {
        return OpenApiDocGatewayFilter(
            resourceLoader,
            openApiProperties.openApiFilesPath,
            writer,
            "${gatewayBasePathProperties.path}${openApiProperties.openApiUri}",
        )
    }
}
