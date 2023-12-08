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

package com.alcosi.nft.apigateway.service.gateway.filter.openapi

import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
@ConditionalOnProperty(prefix = "opendoc",name = ["disabled"], matchIfMissing = true, havingValue = "false")
class OpenApiFiltersConfig {
    @Bean
    @ConditionalOnMissingBean(SwaggerApiGatewayFilter::class)
    fun getSwaggerApiFilter(
        @Value( "\${gateway.base.path:/api}/openapi/docs/swagger-ui/") swaggerUri:String,
        resourceLoader: ResourceLoader,
        @Value("\${openapi.path.resource.swagger:classpath:com/alcosi/nft/apigateway/service/gateway/filter/openapi/swagger_html/}")  swaggerFilePath:String,
        @Value("\${gateway.base.path:/api}\${gateway.openapi.path:/openapi/docs/}\${openapi.path.resource.openApiFile:openapi.yaml}")  openApiFileName:String,
        writer: GatewayFilterResponseWriter,
    ): SwaggerApiGatewayFilter {
        return SwaggerApiGatewayFilter(writer,resourceLoader, swaggerUri,swaggerFilePath,openApiFileName)
    }
    @Bean
   @ConditionalOnMissingBean(OpenApiDocGatewayFilter::class)
    fun getOpenApiFilter(
        @Value( "\${gateway.base.path:/api}\${gateway.openapi.path:/openapi/docs/}") openApiUri:String,
        @Value("\${gateway.base.path:/api}")
        basePath: String,
        resourceLoader: ResourceLoader,
        @Value("\${openapi.path.resource.openapi:/opt/openapi/}")  docFilePath:String,
        @Value("\${spring.cloud.gateway.fake-uri:http://127.0.200.1:87787}")  fakeUri:String,
        writer: GatewayFilterResponseWriter,
        builder: RouteLocatorBuilder
    ): OpenApiDocGatewayFilter {
        return OpenApiDocGatewayFilter(resourceLoader,docFilePath,writer, openApiUri)
    }

}