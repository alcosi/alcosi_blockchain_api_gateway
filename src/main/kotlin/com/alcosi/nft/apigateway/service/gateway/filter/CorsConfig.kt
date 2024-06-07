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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Configuration class for setting up Cross-Origin Resource Sharing (CORS) configuration.
 *
 * This class extends the `CorsConfiguration` class and defines a Spring bean `corsFilter()` that creates
 * a `CorsWebFilter` instance with a default CORS configuration.
 *
 * To enable this CORS configuration, the property `gateway.filter.cors.enabled` must be set to `true`
 * in the application's configuration. If this property is not present, it defaults to `true`.
 *
 * The `corsFilter()` bean is conditionally created using `@ConditionalOnMissingBean` to ensure that
 * it is only created if there is no other bean of type `CorsWebFilter` in the application context.
 *
 * The default CORS configuration allows requests from any origin, allows all HTTP methods, and allows
 * all headers. Requests are not allowed to include credentials.
 *
 */
@ConditionalOnProperty(
    prefix = "gateway.filter.cors",
    name = ["enabled"],
    matchIfMissing = true,
    havingValue = "true",
)
@Configuration
@EnableConfigurationProperties(CorsFilterProperties::class)
class CorsConfig : CorsConfiguration() {
    /**
     * Creates a CorsWebFilter with a default CORS configuration.
     * The default CORS configuration allows requests from any origin, allows all HTTP methods, and allows
     * all headers. Requests are not allowed to include credentials.
     *
     * @return CorsWebFilter - the created CorsWebFilter instance.
     */
    @Bean
    @ConditionalOnMissingBean(CorsWebFilter::class)
    fun corsFilter(): CorsWebFilter {
        val config = CorsConfiguration()
        config.allowCredentials = false
        config.allowedOrigins = listOf("*")
        config.allowedMethods = HttpMethod.values().map { it.name() }
        config.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsWebFilter(source)
    }
}
