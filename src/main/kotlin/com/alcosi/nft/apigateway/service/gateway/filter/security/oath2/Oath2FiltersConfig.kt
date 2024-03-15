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

package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.lib.secured.encrypt.SensitiveComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityFiltersConfig
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityOath2APIGetUserInfoComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityOath2GetUserInfoComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityOath2GetUserInfoService
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityServerProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.web.reactive.function.client.WebClient

@ConditionalOnProperty(matchIfMissing = false, prefix = "filter.config.path.security.type", value = ["method"], havingValue = "IDENTITY_SERVER")
@Configuration
@ConditionalOnBean(SecurityFiltersConfig::class)
@EnableConfigurationProperties(IdentityServerProperties::class)
class Oath2FiltersConfig {
    @Bean
    @ConditionalOnMissingBean(IdentityOath2APIGetUserInfoComponent::class)
    @Lazy
    @ConditionalOnBean(IdentityOath2GetUserInfoService::class)
    fun getOath2APIGetUserInfoComponent(
        mappingHelper: MappingHelper,
        webClient: WebClient,
        oath2AuthComponent: Oath2AuthComponent,
        properties: IdentityServerProperties,
    ): IdentityOath2APIGetUserInfoComponent {
        return IdentityOath2APIGetUserInfoComponent(mappingHelper, webClient, oath2AuthComponent, properties.idApiVersion, properties.uri)
    }

    @Bean
    @ConditionalOnMissingBean(Oath2UserInfoProvider::class)
    fun getOath2GetUserInfoService(
        properties: IdentityServerProperties,
        oath2GetUserInfoComponent: IdentityOath2GetUserInfoComponent,
        oath2APIGetUserInfoComponent: IdentityOath2APIGetUserInfoComponent,
        mappingHelper: MappingHelper,
    ): Oath2UserInfoProvider {
        return IdentityOath2GetUserInfoService(properties.claimClientId, properties.claimOrganisationId, properties.claimType, properties.claimAuthorities, oath2GetUserInfoComponent, oath2APIGetUserInfoComponent, mappingHelper)
    }

    @Bean
    @ConditionalOnMissingBean(JwtGatewayFilter::class)
    fun getOath2JwtGatewayFilter(
        securityGatewayFilter: SecurityGatewayFilter,
        getInfoService: Oath2UserInfoProvider,
        mappingHelper: MappingHelper,
        sensitiveComponent: SensitiveComponent
    ): JwtGatewayFilter {
        return Oath2GatewayFilter(securityGatewayFilter, getInfoService, mappingHelper, sensitiveComponent = sensitiveComponent)
    }

    @Bean
    @ConditionalOnMissingBean(Oath2AuthComponent::class)
    @Lazy
    @ConditionalOnBean(IdentityOath2GetUserInfoService::class)
    fun getOath2AuthComponent(
        webClient: WebClient,
        properties: IdentityServerProperties,
    ): Oath2AuthComponent {
        return Oath2AuthComponent(webClient, properties.uri, properties.clientId, properties.clientSecret, properties.clientScopes.split(" "), properties.grantType, properties.username, properties.password)
    }

    @Bean
    @ConditionalOnMissingBean(IdentityOath2GetUserInfoComponent::class)
    @Lazy
    @ConditionalOnBean(IdentityOath2GetUserInfoService::class)
    fun getOath2GetUserInfoComponent(
        mappingHelper: MappingHelper,
        webClient: WebClient,
        objectMapper: ObjectMapper,
        properties: IdentityServerProperties,
    ): IdentityOath2GetUserInfoComponent {
        return IdentityOath2GetUserInfoComponent(mappingHelper, webClient, objectMapper, properties.uri)
    }
}
