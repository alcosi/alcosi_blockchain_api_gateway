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
import com.alcosi.lib.secured.encrypt.SensitiveComponent
import com.alcosi.lib.secured.encrypt.key.KeyProvider
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.ValidationGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.FilterValidationService
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.breninsul.webfluxlogging.CommonLoggingUtils
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingAutoConfig
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingFilter
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Configuration class for defining various gateway filters.
 */
@AutoConfiguration
@AutoConfigureBefore(SpringCloudGatewayLoggingAutoConfig::class)
@EnableConfigurationProperties(SpringCloudGatewayLoggingProperties::class)
class GatewayFiltersConfig {
    /**
     * Retrieves an instance of CommonLoggingUtils if it is not already defined as a bean.
     *
     * @return The instance of CommonLoggingUtils.
     */
    @Bean
    @ConditionalOnMissingBean(CommonLoggingUtils::class)
    fun getCommonLoggingUtils(): CommonLoggingUtils {
        return CommonLoggingUtils()
    }
    /**
     * Retrieves the SpringCloudGatewayLoggingUtils instance used for logging in the Spring Cloud Gateway.
     *
     * @param props               The SpringCloudGatewayLoggingProperties instance containing the logging properties.
     * @param commonLoggingUtils  The CommonLoggingUtils instance for performing common logging operations.
     * @return The initialized LoggingFilter.Utils instance for logging in the Spring Cloud Gateway.
     */
    @Bean
    @ConditionalOnMissingBean(LoggingFilter.Utils::class)
    fun getSpringCloudGatewayLoggingUtils(
        props: SpringCloudGatewayLoggingProperties,
        commonLoggingUtils: CommonLoggingUtils,
    ): LoggingFilter.Utils {
        val logger =
            java.util.logging.Logger
                .getLogger(props.loggerClass)
        return LoggingFilter.Utils(props.maxBodySize, logger, props.getLoggingLevelAsJavaLevel(), props.logTime, props.logHeaders, props.logBody, commonLoggingUtils)
    }

    /**
     * Retrieves the SpringCloudGatewayLoggingFilter instance.
     *
     * @param springCloudGatewayLoggingUtils The SpringCloudGatewayLoggingUtils instance used for logging.
     * @param encryptFilters The list of EncryptGatewayFilter instances.
     * @param decryptFilters The list of DecryptGatewayFilter instances.
     * @return The initialized SpringCloudGatewayLoggingFilter instance.
     */
    @Bean
    @ConditionalOnMissingBean(SpringCloudGatewayLoggingFilter::class)
    fun getSpringCloudGatewayLoggingFilter(
        springCloudGatewayLoggingUtils: LoggingFilter.Utils,
        encryptFilters: List<EncryptGatewayFilter>,
        decryptFilters: List<DecryptGatewayFilter>,
    ): SpringCloudGatewayLoggingFilter {
        val maxFromEncryptFilters = (encryptFilters + decryptFilters).map { it.order + 1 } + listOf(Int.MIN_VALUE)
        val orderVal = maxFromEncryptFilters.max()
        return LoggingFilter(true, springCloudGatewayLoggingUtils, orderVal, HeaderHelper.RQ_ID)
    }

    /**
     * Retrieves an instance of the GatewayFilterResponseWriter.
     *
     * @param mappingHelper The ObjectMapper instance used for serialization and deserialization of JSON.
     * @return An instance of the GatewayFilterResponseWriter.
     */
    @Bean
    @ConditionalOnMissingBean(GatewayFilterResponseWriter::class)
    fun getGatewayFilterResponseWriter(mappingHelper: ObjectMapper): GatewayFilterResponseWriter = GatewayFilterResponseWriter(mappingHelper)

    /**
     * Retrieves an instance of the StripBaseUriFilter.
     *
     * @param basePath The base path used for filtering.
     * @return An instance of the StripBaseUriFilter.
     */
    @Bean
    @ConditionalOnMissingBean(StripBaseUriFilter::class)
    fun getStripBaseUriFilter(
        @Value("\${gateway.base.path:/api}") basePath: String,
    ): StripBaseUriFilter = StripBaseUriFilter(basePath)

    /**
     * Retrieves an instance of the MultipartToJsonGatewayFilter.
     *
     * @param writer The GatewayFilterResponseWriter instance used for writing the response.
     * @param objectMapper The ObjectMapper instance used for serialization and deserialization of JSON.
     * @return An instance of the MultipartToJsonGatewayFilter.
     */
    @Bean
    @ConditionalOnMissingBean(MultipartToJsonGatewayFilter::class)
    fun getMultipartToJsonGatewayFilter(
        writer: GatewayFilterResponseWriter,
        objectMapper: ObjectMapper,
    ): MultipartToJsonGatewayFilter = MultipartToJsonGatewayFilter(objectMapper = objectMapper)

    /**
     * Retrieves an instance of the EncryptGatewayFilter.
     *
     * @param commonUtils The CommonLoggingUtils instance used for common logging operations.
     * @param keyProvider The KeyProvider instance used for retrieving encryption keys.
     * @param objectMapper The ObjectMapper instance used for serialization and deserialization of JSON.
     * @return An instance of the EncryptGatewayFilter.
     */
    @Bean
    @ConditionalOnMissingBean(EncryptGatewayFilter::class)
    fun getEncryptGatewayFilter(
        commonUtils: CommonLoggingUtils,
        keyProvider: KeyProvider,
        objectMapper: ObjectMapper,
    ): EncryptGatewayFilter = EncryptGatewayFilter(commonUtils, keyProvider, objectMapper, PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD)

    /**
     * Retrieves an instance of the DecryptGatewayFilter.
     *
     * @param commonUtils The CommonLoggingUtils instance used for common logging operations.
     * @param sensitiveComponent The SensitiveComponent used for decryption.
     * @param keyProvider The KeyProvider used for obtaining the decryption key.
     * @return An instance of the DecryptGatewayFilter.
     */
    @Bean
    @ConditionalOnMissingBean(DecryptGatewayFilter::class)
    fun getDecryptGatewayFilter(
        commonUtils: CommonLoggingUtils,
        sensitiveComponent: SensitiveComponent,
        keyProvider: KeyProvider,
    ): DecryptGatewayFilter = DecryptGatewayFilter(commonUtils, sensitiveComponent, keyProvider, attrProxyConfigField =  PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD)

    /**
     * Retrieves an instance of the `ValidationGatewayFilter`.
     *
     * @param validationService The `FilterValidationService` used for request validation.
     * @param pathConfig The `PathConfigurationComponent` used for path configuration.
     * @param basePath The base path used for filtering.
     * @return An instance of the `ValidationGatewayFilter`.
     */
    @Bean
    @ConditionalOnMissingBean(ValidationGatewayFilter::class)
    fun getValidationGatewayFilter(
        validationService: FilterValidationService,
        pathConfig: PathConfigurationComponent,
        @Value("\${gateway.base.path:/api}") basePath: String,
    ): ValidationGatewayFilter = ValidationGatewayFilter(validationService, pathConfig.validationConfig.toPredicate())

    /**
     * Retrieves the ContextHeadersGatewayFilter instance.
     *
     * @param serviceName The name of the microservice.
     * @param environment The environment of the microservice.
     * @return The initialized ContextHeadersGatewayFilter instance.
     */
    @Bean
    @ConditionalOnMissingBean(ContextHeadersGatewayFilter::class)
    fun getContextHeadersGatewayFilter(
        @Value("\${spring.application.name:API_GATEWAY}") serviceName: String,
        @Value("\${spring.application.environment:dev}") environment: String,
    ): ContextHeadersGatewayFilter = ContextHeadersGatewayFilter(serviceName, environment)
}
