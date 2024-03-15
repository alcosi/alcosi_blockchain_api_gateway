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

package com.alcosi.nft.apigateway.service.gateway.filter.security.eth

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.auth.service.CheckJWTService
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityFiltersConfig
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnBean(SecurityFiltersConfig::class)
@ConditionalOnProperty(matchIfMissing = true, prefix = "filter.config.path.security.type", value = ["method"], havingValue = "ETH_JWT")
class EthJwtFiltersConfig {
    @Bean
    @ConditionalOnMissingBean(JwtGatewayFilter::class)
    fun getEthJwtGatewayFilter(
        securityGatewayFilter: SecurityGatewayFilter,
        checkJWTService: CheckJWTService,
        mappingHelper: MappingHelper,
    ): JwtGatewayFilter {
        return EthJwtGatewayFilter(securityGatewayFilter, checkJWTService)
    }
}
