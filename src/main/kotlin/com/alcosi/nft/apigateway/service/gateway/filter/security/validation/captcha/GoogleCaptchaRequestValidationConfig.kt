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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.captcha

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationProperties
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@EnableConfigurationProperties(GoogleCaptchaProperties::class)
@ConditionalOnProperty(prefix = "validation.google.captcha", name = ["disabled"], matchIfMissing = true, havingValue = "false")
class GoogleCaptchaRequestValidationConfig {
    @Bean
    @ConditionalOnMissingBean(GoogleCaptchaRequestValidationComponent::class)
    fun getGoogleCaptchaComponent(
        properties: GoogleCaptchaProperties,
        webClient: WebClient,
        mappingHelper: MappingHelper,
        validationUniqueTokenChecker: ValidationUniqueTokenChecker,
    ): GoogleCaptchaRequestValidationComponent {
        return GoogleCaptchaRequestValidationComponent(
            properties.alwaysPassed,
            properties.superTokenEnabled,
            properties.superUserToken,
            properties.ttl,
            properties.key,
            properties.minRate,
            properties.uri,
            webClient,
            mappingHelper,
            validationUniqueTokenChecker,
        )
    }

    @Bean
    @ConditionalOnMissingBean(GoogleCaptchaValidator::class)
    fun getGoogleCaptchaValidator(
        component: GoogleCaptchaRequestValidationComponent,
        validationProperties: ValidationProperties,
    ): GoogleCaptchaValidator {
        return GoogleCaptchaValidator(component, validationProperties.tokenHeader, validationProperties.ipHeader)
    }
}
