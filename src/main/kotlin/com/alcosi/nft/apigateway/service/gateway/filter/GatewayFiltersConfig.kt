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
import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.lib.secured.encrypt.SensitiveComponent
import com.alcosi.lib.secured.encrypt.key.KeyProvider
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.ValidationGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.FilterValidationService
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.breninsul.webfluxlogging.CommonLoggingUtils
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingAutoConfig
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingFilter
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
@AutoConfigureBefore(SpringCloudGatewayLoggingAutoConfig::class)
class GatewayFiltersConfig {
    @Bean
    fun getSpringCloudGatewayLoggingFilter(
        springCloudGatewayLoggingUtils: SpringCloudGatewayLoggingUtils,
        encryptFilters: List<EncryptGatewayFilter>,
        decryptFilters: List<DecryptGatewayFilter>,
    ): SpringCloudGatewayLoggingFilter {
        val maxFromEncryptFilters = (encryptFilters + decryptFilters).map { it.order + 1 } + listOf(Int.MIN_VALUE)
        val orderVal = maxFromEncryptFilters.max()
        return SpringCloudGatewayLoggingFilter(true, springCloudGatewayLoggingUtils, orderVal, HeaderHelper.RQ_ID)
    }

    @Bean
    fun getGatewayFilterResponseWriter(mappingHelper: MappingHelper): GatewayFilterResponseWriter {
        return GatewayFilterResponseWriter(mappingHelper)
    }

    @Bean
    fun getStripBaseUriFilter(
        @Value("\${gateway.base.path:/api}") basePath: String,
    ): StripBaseUriFilter {
        return StripBaseUriFilter(basePath)
    }

    @Bean
    @ConditionalOnMissingBean(MultipartToJsonGatewayFilter::class)
    fun getMultipartToJsonGatewayFilter(
        writer: GatewayFilterResponseWriter,
        mappingHelper: MappingHelper,
    ): MultipartToJsonGatewayFilter {
        return MultipartToJsonGatewayFilter()
    }

    @Bean
    @ConditionalOnMissingBean(EncryptGatewayFilter::class)
    fun getEncryptGatewayFilter(
        commonUtils: CommonLoggingUtils,
        keyProvider: KeyProvider,
        objectMapper: ObjectMapper,
    ): EncryptGatewayFilter {
        return EncryptGatewayFilter(commonUtils, keyProvider, objectMapper, PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD)
    }

    @Bean
    @ConditionalOnMissingBean(DecryptGatewayFilter::class)
    fun getDecryptGatewayFilter(
        commonUtils: CommonLoggingUtils,
        sensitiveComponent: SensitiveComponent,
        keyProvider: KeyProvider,
    ): DecryptGatewayFilter {
        return DecryptGatewayFilter(commonUtils, sensitiveComponent, keyProvider)
    }

    @Bean
    @ConditionalOnMissingBean(ValidationGatewayFilter::class)
    fun getValidationGatewayFilter(
        validationService: FilterValidationService,
        pathConfig: PathConfigurationComponent,
        @Value("\${gateway.base.path:/api}") basePath: String,
    ): ValidationGatewayFilter {
        return ValidationGatewayFilter(validationService, pathConfig.validationConfig.toPredicate())
    }

    @Bean
    @ConditionalOnMissingBean(ContextHeadersGatewayFilter::class)
    fun getContextHeadersGatewayFilter(
        @Value("\${spring.application.name:API_GATEWAY}") serviceName: String,
        @Value("\${spring.application.environment:dev}") environment: String,
    ): ContextHeadersGatewayFilter {
        return ContextHeadersGatewayFilter(serviceName, environment)
    }
}
