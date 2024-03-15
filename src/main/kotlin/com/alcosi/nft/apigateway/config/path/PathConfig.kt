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

package com.alcosi.nft.apigateway.config.path

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.GatewayBasePathProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    PathConfigurationProperties::class,
    SecurityRoutesProperties::class,
    GatewayBasePathProperties::class,
)
open class PathConfig {
    @Bean
    fun getPathConfig(
        properties: PathConfigurationProperties,
        securityRoutesProperties: SecurityRoutesProperties,
        helper: MappingHelper,
        objectMapper: ObjectMapper,
        gatewayBasePathProperties: GatewayBasePathProperties,
    ): PathConfigurationComponent {
        return PathConfigurationComponent(properties, securityRoutesProperties.security, securityRoutesProperties.validation, helper, objectMapper, gatewayBasePathProperties.path)
    }
}
