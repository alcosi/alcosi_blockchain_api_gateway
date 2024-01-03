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

package com.alcosi.nft.apigateway.service.gateway.filter

import com.alcosi.lib.filters.HeaderHelper
import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.lib.secured.encrypt.SensitiveComponent
import com.alcosi.lib.secured.encrypt.key.KeyProvider
import com.alcosi.nft.apigateway.config.PathConfig
import com.alcosi.nft.apigateway.config.PathConfigProperties
import com.alcosi.nft.apigateway.service.gateway.filter.security.ValidationGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.FilterValidationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.breninsul.webfluxlogging.CommonLoggingUtils
import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingFilter
import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class GatewayFiltersConfig {
    @Bean
    fun getPathConfig(
        properties: PathConfigProperties,
        helper: MappingHelper,
        objectMapper: ObjectMapper,
        @Value("\${gateway.base.path:/api}") basePath: String
    ): PathConfig {
        return PathConfig(properties, helper, objectMapper, basePath)
    }
    @Bean
    fun getSpringCloudGatewayLoggingFilter(
        springCloudGatewayLoggingUtils: SpringCloudGatewayLoggingUtils,
        encryptFilters:List<EncryptGatewayFilter>,
        decryptFilters: List<DecryptGatewayFilter>
    ): SpringCloudGatewayLoggingFilter {
        val maxFromEncryptFilters = (encryptFilters + decryptFilters).map { it.order + 1 }+ listOf(Int.MIN_VALUE)
        val orderVal= maxFromEncryptFilters.max()
        return SpringCloudGatewayLoggingFilter(true,springCloudGatewayLoggingUtils, orderVal,HeaderHelper.RQ_ID)
    }
    @Bean
    fun getGatewayFilterResponseWriter(mappingHelper: MappingHelper): GatewayFilterResponseWriter {
        return GatewayFilterResponseWriter(mappingHelper)
    }

    @Bean
    fun getStripBaseUriFilter(
        @Value("\${gateway.base.path:/api}") basePath: String
    ): StripBaseUriFilter {
        return StripBaseUriFilter(basePath)
    }

    @Bean
    @ConditionalOnMissingBean(MultipartToJsonGatewayFilter::class)
    fun getMultipartToJsonGatewayFilter(
        writer: GatewayFilterResponseWriter,
        mappingHelper: MappingHelper
    ): MultipartToJsonGatewayFilter {
        return MultipartToJsonGatewayFilter()
    }


    @Bean
    @ConditionalOnMissingBean(EncryptGatewayFilter::class)
    fun getEncryptGatewayFilter(
        commonUtils: CommonLoggingUtils,
        keyProvider: KeyProvider,
        objectMapper: ObjectMapper
    ): EncryptGatewayFilter {
        return EncryptGatewayFilter(commonUtils,keyProvider, objectMapper,PathConfig.ATTRIBUTE_PROXY_CONFIG_FIELD)
    }
    @Bean
    @ConditionalOnMissingBean(DecryptGatewayFilter::class)
    fun getDecryptGatewayFilter(
        commonUtils: CommonLoggingUtils,
         sensitiveComponent: SensitiveComponent,
        keyProvider: KeyProvider,
    ): DecryptGatewayFilter {
        return DecryptGatewayFilter(commonUtils,sensitiveComponent,keyProvider)
    }
    @Bean
    @ConditionalOnMissingBean(ValidationGatewayFilter::class)
    fun getValidationGatewayFilter(
        validationService: FilterValidationService,
        pathConfig: PathConfig,
        @Value("\${gateway.base.path:/api}") basePath: String,
    ): ValidationGatewayFilter {
        return ValidationGatewayFilter(validationService, pathConfig.validationConfig.toPredicate())
    }
    @Bean
    @ConditionalOnMissingBean(ContextHeadersGatewayFilter::class)
    fun getContextHeadersGatewayFilter(
        @Value("\${spring.application.name:API_GATEWAY}")  serviceName: String,
        @Value("\${spring.application.environment:dev}")  environment: String,
    ): ContextHeadersGatewayFilter {
        return ContextHeadersGatewayFilter(serviceName, environment)
    }
}