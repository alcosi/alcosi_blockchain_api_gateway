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

package com.alcosi.nft.apigateway.service.gateway.filter.security

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for SecurityFiltersConfig.
 *
 * This class is responsible for configuring the security filters for the application based on the provided properties.
 */
@Configuration
@ConditionalOnProperty(matchIfMissing = true, prefix = "filter.config.path.security", value = ["enabled"], havingValue = "true")
class SecurityFiltersConfig {
    /**
     * Retrieves the SecurityGatewayFilter based on the provided path configuration.
     *
     * @param pathConfig The PathConfigurationComponent containing the security configuration path.
     * @return The SecurityGatewayFilter configured with the provided path configuration.
     */
    @Bean
    @ConditionalOnMissingBean(SecurityGatewayFilter::class)
    fun getSecurityGatewayFilter(pathConfig: PathConfigurationComponent): SecurityGatewayFilter {
        return SecurityGatewayFilter(pathConfig.securityConfig.toPredicate(), PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD)
    }
}
