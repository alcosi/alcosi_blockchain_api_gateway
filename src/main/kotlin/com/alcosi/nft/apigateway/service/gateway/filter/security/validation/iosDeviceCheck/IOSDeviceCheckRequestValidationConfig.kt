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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.iosDeviceCheck

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
    prefix = "validation.ios.device-check",
    name = ["disabled"],
    matchIfMissing = true,
    havingValue = "false",
)
@EnableConfigurationProperties(IOSDeviceCheckProperties::class)
class IOSDeviceCheckRequestValidationConfig {
    @Bean
    @ConditionalOnMissingBean(IOSDeviceCheckJWTComponent::class)
    fun getIOSDeviceCheckJWTComponent(properties: IOSDeviceCheckProperties): IOSDeviceCheckJWTComponent {
        val jwt = properties.jwt
        return IOSDeviceCheckJWTComponent(jwt.audenceUri, jwt.ttl, jwt.keyId, jwt.issuer, jwt.subject, jwt.privateKey)
    }

    @Bean
    @ConditionalOnMissingBean(IOSDeviceCheckRequestValidationComponent::class)
    fun getIOSDeviceCheckRequestValidationComponent(
        properties: IOSDeviceCheckProperties,
        webClient: WebClient,
        mappingHelper: MappingHelper,
        jwtComponent: IOSDeviceCheckJWTComponent,
        uniqueTokenChecker: ValidationUniqueTokenChecker,
    ): IOSDeviceCheckRequestValidationComponent {
        return IOSDeviceCheckRequestValidationComponent(properties.alwaysPassed, properties.superTokenEnabled, properties.superUserToken, properties.ttl, properties.uri, webClient, mappingHelper, jwtComponent, uniqueTokenChecker)
    }

    @Bean
    @ConditionalOnMissingBean(IOSDeviceCheckValidator::class)
    fun getIOSDeviceCheckValidator(
        component: IOSDeviceCheckRequestValidationComponent,
        validationProperties: ValidationProperties,
    ): IOSDeviceCheckValidator {
        return IOSDeviceCheckValidator(component, validationProperties.tokenHeader, validationProperties.ipHeader)
    }
}
