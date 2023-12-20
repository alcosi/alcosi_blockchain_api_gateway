/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityFiltersConfig
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityServerProperties
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityOath2APIGetUserInfoComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityOath2GetUserInfoComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity.IdentityOath2GetUserInfoService
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
        return IdentityOath2GetUserInfoService(properties.claimClientId, properties.claimType, properties.claimAuthorities, oath2GetUserInfoComponent, oath2APIGetUserInfoComponent, mappingHelper)
    }

    @Bean
    @ConditionalOnMissingBean(JwtGatewayFilter::class)
    fun getOath2JwtGatewayFilter(
        securityGatewayFilter: SecurityGatewayFilter,
        getInfoService: Oath2UserInfoProvider,
        mappingHelper: MappingHelper,
    ): JwtGatewayFilter {
        return Oath2GatewayFilter(securityGatewayFilter, getInfoService, mappingHelper)
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
