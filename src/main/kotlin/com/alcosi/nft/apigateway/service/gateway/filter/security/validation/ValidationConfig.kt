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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation

import io.github.breninsul.synchronizationstarter.service.SynchronizationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The ValidationConfig class is a configuration class used to configure and create beans for validation components.
 * It is responsible for initializing and wiring the necessary dependencies required for validation.
 */
@Configuration
@EnableConfigurationProperties(ValidationProperties::class)
@ConditionalOnProperty(prefix = "validation", name = ["disabled"], matchIfMissing = true, havingValue = "false")
class ValidationConfig {
    /**
     * Retrieves an instance of FilterValidationService based on the provided dependencies.
     *
     * @param services The list of RequestValidators to use for validation.
     * @param properties The validation properties containing configuration values.
     * @return An instance of FilterValidationService.
     */
    @Bean
    @ConditionalOnMissingBean(FilterValidationService::class)
    fun getFilterValidationService(
        services: List<RequestValidator>,
        properties: ValidationProperties,
    ): FilterValidationService {
        return FilterValidationService(services, properties.alwaysPassed, properties.tokenTypeHeader)
    }

    /**
     * Retrieves an instance of ValidationUniqueTokenChecker based on the provided dependencies.
     *
     * @param synchronizationService The synchronization service used for token synchronization.
     * @return An instance of ValidationUniqueTokenChecker.
     */
    @Bean
    @ConditionalOnMissingBean(ValidationUniqueTokenChecker::class)
    fun getValidationUniqueTokenChecker(synchronizationService: SynchronizationService): ValidationUniqueTokenChecker {
        return ValidationUniqueTokenChecker(synchronizationService)
    }
}
