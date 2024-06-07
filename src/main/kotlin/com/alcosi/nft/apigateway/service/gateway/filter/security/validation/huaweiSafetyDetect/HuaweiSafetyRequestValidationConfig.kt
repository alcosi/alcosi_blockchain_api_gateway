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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.huaweiSafetyDetect

import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationProperties
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration class for Huawei Safety Request Validation.
 *
 * This class provides the configuration for the Huawei Safety Request Validation component,
 * including the conditional bean creation for the necessary components.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "validation.huawei.safety-detect",
    name = ["disabled"],
    matchIfMissing = true,
    havingValue = "false",
)
@EnableConfigurationProperties(HuaweiSafetyDetectProperties::class)
class HuaweiSafetyRequestValidationConfig {
    /**
     * Returns a new instance of HuaweiSafetyVerifySignatureComponent based on the provided properties.
     *
     * @param properties The HuaweiSafetyDetectProperties object containing the certificate information.
     * @return The HuaweiSafetyVerifySignatureComponent instance initialized with the provided certificate.
     */
    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyVerifySignatureComponent::class)
    fun getHuaweiSafetyVerifySignatureComponent(properties: HuaweiSafetyDetectProperties): HuaweiSafetyVerifySignatureComponent {
        return HuaweiSafetyVerifySignatureComponent(properties.certificate)
    }

    /**
     * Returns the HuaweiSafetyDetectRequestValidationComponent based on the provided properties.
     *
     * @param properties The HuaweiSafetyDetectProperties object containing the configuration information.
     * @param webClient The WebClient instance for making HTTP requests.
     * @param mappingHelper The ObjectMapper instance for mapping objects.
     * @param verifyUtil The HuaweiSafetyVerifySignatureComponent instance for signature verification.
     * @param uniqueTokenChecker The ValidationUniqueTokenChecker instance for checking token uniqueness.
     * @return The HuaweiSafetyDetectRequestValidationComponent initialized with the provided properties.
     */
    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyDetectRequestValidationComponent::class)
    fun getHuaweiSafetyDetectRequestValidationComponent(
        properties: HuaweiSafetyDetectProperties,
        webClient: WebClient,
        mappingHelper: ObjectMapper,
        verifyUtil: HuaweiSafetyVerifySignatureComponent,
        uniqueTokenChecker: ValidationUniqueTokenChecker,
    ): HuaweiSafetyDetectRequestValidationComponent {
        return HuaweiSafetyDetectRequestValidationComponent(
            properties.alwaysPassed,
            properties.superTokenEnabled,
            properties.superUserToken,
            properties.ttl,
            properties.packageName,
            webClient,
            mappingHelper,
            verifyUtil,
            uniqueTokenChecker,
        )
    }

    /**
     * Retrieves an instance of HuaweiSafetyDetectCheckValidator.
     *
     * @param component The HuaweiSafetyDetectRequestValidationComponent used for request validation.
     * @param validationProperties The validation properties.
     * @return The HuaweiSafetyDetectCheckValidator instance.
     */
    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyDetectCheckValidator::class)
    fun getHuaweiSafetyDetectCheckValidator(
        component: HuaweiSafetyDetectRequestValidationComponent,
        validationProperties: ValidationProperties,
    ): HuaweiSafetyDetectCheckValidator {
        return HuaweiSafetyDetectCheckValidator(component, validationProperties.tokenHeader, validationProperties.ipHeader)
    }
}
