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
@ConditionalOnProperty(
    prefix = "validation.huawei.safety-detect",
    name = ["disabled"],
    matchIfMissing = true,
    havingValue = "false",
)
@EnableConfigurationProperties(HuaweiSafetyDetectProperties::class)
class HuaweiSafetyRequestValidationConfig {
    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyVerifySignatureComponent::class)
    fun getHuaweiSafetyVerifySignatureComponent(properties: HuaweiSafetyDetectProperties): HuaweiSafetyVerifySignatureComponent {
        return HuaweiSafetyVerifySignatureComponent(properties.certificate)
    }

    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyDetectRequestValidationComponent::class)
    fun getHuaweiSafetyDetectRequestValidationComponent(
        properties: HuaweiSafetyDetectProperties,
        webClient: WebClient,
        mappingHelper: MappingHelper,
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

    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyDetectCheckValidator::class)
    fun getHuaweiSafetyDetectCheckValidator(
        component: HuaweiSafetyDetectRequestValidationComponent,
        validationProperties: ValidationProperties,
    ): HuaweiSafetyDetectCheckValidator {
        return HuaweiSafetyDetectCheckValidator(component, validationProperties.tokenHeader, validationProperties.ipHeader)
    }
}
