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

import com.alcosi.lib.synchronisation.SynchronizationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ValidationProperties::class)
@ConditionalOnProperty(prefix = "validation", name = ["disabled"], matchIfMissing = true, havingValue = "false")
class ValidationConfig {
    @Bean
    @ConditionalOnMissingBean(FilterValidationService::class)
    fun getFilterValidationService(
        services: List<RequestValidator>,
        properties: ValidationProperties,
    ): FilterValidationService {
        return FilterValidationService(services, properties.alwaysPassed, properties.tokenTypeHeader)
    }

    @Bean
    @ConditionalOnMissingBean(ValidationUniqueTokenChecker::class)
    fun getValidationUniqueTokenChecker(synchronizationService: SynchronizationService): ValidationUniqueTokenChecker {
        return ValidationUniqueTokenChecker(synchronizationService)
    }
}
