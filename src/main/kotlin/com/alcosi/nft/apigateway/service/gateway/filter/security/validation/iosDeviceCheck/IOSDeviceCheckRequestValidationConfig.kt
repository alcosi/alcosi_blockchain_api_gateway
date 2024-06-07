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
 * The `IOSDeviceCheckRequestValidationConfig` class represents the configuration for iOS device check request validation.
 *
 * This class is marked with the `@Configuration` annotation to indicate that it is a source of bean definitions for the application context.
 * It is also annotated with `@ConditionalOnProperty` to conditionally enable the configuration based on a property value.
 * The `@EnableConfigurationProperties` annotation is used to enable support for `IOSDeviceCheckProperties`.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "validation.ios.device-check",
    name = ["disabled"],
    matchIfMissing = true,
    havingValue = "false",
)
@EnableConfigurationProperties(IOSDeviceCheckProperties::class)
class IOSDeviceCheckRequestValidationConfig {
    /**
     * Retrieves an instance of the `IOSDeviceCheckJWTComponent` class based on the provided properties.
     *
     * @param properties The `IOSDeviceCheckProperties` object containing the configuration properties for generating JWTs.
     * @return An instance of the `IOSDeviceCheckJWTComponent` class.
     */
    @Bean
    @ConditionalOnMissingBean(IOSDeviceCheckJWTComponent::class)
    fun getIOSDeviceCheckJWTComponent(properties: IOSDeviceCheckProperties): IOSDeviceCheckJWTComponent {
        val jwt = properties.jwt
        return IOSDeviceCheckJWTComponent(jwt.audenceUri, jwt.ttl, jwt.keyId, jwt.issuer, jwt.subject, jwt.privateKey)
    }

    /**
     * Returns an instance of `IOSDeviceCheckRequestValidationComponent` based on the provided properties and dependencies.
     *
     * @param properties The `IOSDeviceCheckProperties` object containing the configuration properties.
     * @param webClient The `WebClient` used for making HTTP requests to the Apple server.
     * @param mappingHelper The `MappingHelper` used for mapping JSON responses.
     * @param jwtComponent The `IOSDeviceCheckJWTComponent` used for generating JWTs.
     * @param uniqueTokenChecker The `ValidationUniqueTokenChecker` used for checking token uniqueness.
     * @return An instance of the `IOSDeviceCheckRequestValidationComponent`.
     */
    @Bean
    @ConditionalOnMissingBean(IOSDeviceCheckRequestValidationComponent::class)
    fun getIOSDeviceCheckRequestValidationComponent(
        properties: IOSDeviceCheckProperties,
        webClient: WebClient,
        mappingHelper: ObjectMapper,
        jwtComponent: IOSDeviceCheckJWTComponent,
        uniqueTokenChecker: ValidationUniqueTokenChecker,
    ): IOSDeviceCheckRequestValidationComponent {
        return IOSDeviceCheckRequestValidationComponent(properties.alwaysPassed, properties.superTokenEnabled, properties.superUserToken, properties.ttl, properties.uri, webClient, mappingHelper, jwtComponent, uniqueTokenChecker)
    }

    /**
     * Retrieves an instance of the `IOSDeviceCheckValidator` class based on the provided components and properties.
     *
     * @param component The `IOSDeviceCheckRequestValidationComponent` component used for validation.
     * @param validationProperties The properties containing the token and IP header for validation.
     * @return An instance of the `IOSDeviceCheckValidator` class.
     */
    @Bean
    @ConditionalOnMissingBean(IOSDeviceCheckValidator::class)
    fun getIOSDeviceCheckValidator(
        component: IOSDeviceCheckRequestValidationComponent,
        validationProperties: ValidationProperties,
    ): IOSDeviceCheckValidator {
        return IOSDeviceCheckValidator(component, validationProperties.tokenHeader, validationProperties.ipHeader)
    }
}
