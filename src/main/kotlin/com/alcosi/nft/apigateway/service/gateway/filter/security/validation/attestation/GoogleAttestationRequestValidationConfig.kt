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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.attestation

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationProperties
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@EnableConfigurationProperties(GoogleAttestationProperties::class)
@Configuration
@ConditionalOnProperty(prefix = "validation.google-attestation", name = ["disabled"], matchIfMissing = true, havingValue = "false")
class GoogleAttestationRequestValidationConfig {
    @Bean
    @ConditionalOnMissingBean(GoogleAttestationRequestValidationComponent::class)
    @ConditionalOnProperty(prefix = "validation.google.attestation", name = ["type"], matchIfMissing = true, havingValue = "ONLINE")
    fun getGoogleOnlineAttestationComponent(
        webClient: WebClient,
        mappingHelper: MappingHelper,
        uniqueTokenChecker: ValidationUniqueTokenChecker,
        properties: GoogleAttestationProperties,
    ): GoogleAttestationRequestValidationComponent {
        return GoogleAttestationOnlineRequestValidationComponent(
            properties.alwaysPassed,
            properties.superTokenEnabled,
            properties.superUserToken,
            properties.key,
            properties.packageName,
            properties.ttl,
            properties.uri,
            webClient,
            mappingHelper,
            uniqueTokenChecker,
        )
    }

    @Bean
    @ConditionalOnMissingBean(GoogleAttestationRequestValidationComponent::class)
    @ConditionalOnProperty(prefix = "validation.google.attestation", name = ["type"], matchIfMissing = false, havingValue = "OFFLINE")
    fun getGoogleOfflineAttestationComponent(
        mappingHelper: MappingHelper,
        uniqueTokenChecker: ValidationUniqueTokenChecker,
        properties: GoogleAttestationProperties,
    ): GoogleAttestationRequestValidationComponent {
        return GoogleAttestationOfflineRequestValidationComponent(
            properties.alwaysPassed,
            properties.superTokenEnabled,
            properties.superUserToken,
            properties.key,
            properties.packageName,
            properties.ttl,
            properties.hostname,
            mappingHelper,
            uniqueTokenChecker,
        )
    }

    @Bean
    @ConditionalOnMissingBean(GoogleAttestationValidator::class)
    fun getGoogleAttestationValidator(
        component: GoogleAttestationRequestValidationComponent,
        validationProperties: ValidationProperties,
    ): GoogleAttestationValidator {
        return GoogleAttestationValidator(component, validationProperties.tokenHeader, validationProperties.ipHeader)
    }
}
