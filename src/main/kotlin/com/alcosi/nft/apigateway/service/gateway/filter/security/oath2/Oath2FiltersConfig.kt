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

/**
 * Configuration class for configuring OAuth2 filters.
 *
 * This class is responsible for configuring and providing various beans related to OAuth2 filters, such as
 * [IdentityOath2APIGetUserInfoComponent], [Oath2UserInfoProvider], [JwtGatewayFilter],
 * [Oath2AuthComponent], and [IdentityOath2GetUserInfoComponent].
 *
 * This class is annotated with [@Configuration][Configuration] to indicate that it is a configuration class.
 * Each bean method inside the class is annotated with [@Bean][Bean] to indicate that it is responsible for providing a bean.
 *
 * To use this class, make sure the required dependencies are available and autowire the desired bean.
 *
 * @see ConditionalOnProperty
 * @see ConditionalOnBean
 * @see EnableConfigurationProperties
 * @see Bean
 * @see ConditionalOnMissingBean
 * @see Lazy
 * @see WebClient
 * @see IdentityServerProperties
 * @see MappingHelper
 * @see Oath2AuthComponent
 * @see SecurityGatewayFilter
 * @see Oath2GatewayFilter
 * @see Oath2UserInfoProvider
 * @see IdentityOath2APIGetUserInfoComponent
 * @see IdentityOath2GetUserInfoComponent
 */
@ConditionalOnProperty(matchIfMissing = false, prefix = "filter.config.path.security.type", value = ["method"], havingValue = "IDENTITY_SERVER")
@Configuration
@ConditionalOnBean(SecurityFiltersConfig::class)
@EnableConfigurationProperties(IdentityServerProperties::class)
class Oath2FiltersConfig {
    /**
     * Retrieves user information based on the provided ID.
     *
     * @param id The ID of the user.
     * @return A Mono that emits the User object.
     * @throws ApiException if there is an error retrieving the user information.
     */
    @Bean
    @ConditionalOnMissingBean(IdentityOath2APIGetUserInfoComponent::class)
    @Lazy
//    @ConditionalOnBean(IdentityOath2GetUserInfoService::class)
    fun getOath2APIGetUserInfoComponent(
        mappingHelper: ObjectMapper,
        webClient: WebClient,
        oath2AuthComponent: Oath2AuthComponent,
        properties: IdentityServerProperties,
    ): IdentityOath2APIGetUserInfoComponent {
        return IdentityOath2APIGetUserInfoComponent(mappingHelper, webClient, oath2AuthComponent, properties.idApiVersion, properties.uri!!)
    }

    /**
     * Retrieves the OAuth2 user information service.
     *
     * @param properties The IdentityServerProperties object containing the configuration properties for the identity server.
     * @param oath2GetUserInfoComponent The IdentityOath2GetUserInfoComponent object for retrieving user information.
     * @param oath2APIGetUserInfoComponent The IdentityOath2APIGetUserInfoComponent object for accessing the OAuth2 API.
     * @param mappingHelper The MappingHelper object for mapping user information.
     * @return The Oath2UserInfoProvider object that retrieves user information based on the provided configuration.
     */
    @Bean
    @ConditionalOnMissingBean(Oath2UserInfoProvider::class)
    fun getOath2GetUserInfoService(
        properties: IdentityServerProperties,
        oath2GetUserInfoComponent: IdentityOath2GetUserInfoComponent,
        oath2APIGetUserInfoComponent: IdentityOath2APIGetUserInfoComponent,
        mappingHelper: ObjectMapper,
    ): Oath2UserInfoProvider {
        return IdentityOath2GetUserInfoService(properties.claimClientId, properties.claimOrganisationId, properties.claimType, properties.claimAuthorities, oath2GetUserInfoComponent, oath2APIGetUserInfoComponent, mappingHelper)
    }

    /**
     * Retrieves the OAuth2 JWT Gateway Filter.
     *
     * @param securityGatewayFilter The SecurityGatewayFilter instance.
     * @param getInfoService The Oath2UserInfoProvider instance.
     * @param mappingHelper The MappingHelper instance.
     * @param sensitiveComponent The SensitiveComponent instance.
     * @return The JwtGatewayFilter instance.
     */
    @Bean
    @ConditionalOnMissingBean(JwtGatewayFilter::class)
    fun getOath2JwtGatewayFilter(
        securityGatewayFilter: SecurityGatewayFilter,
        getInfoService: Oath2UserInfoProvider,
        mappingHelper: ObjectMapper,
        sensitiveComponent: SensitiveComponent
    ): JwtGatewayFilter {
        return Oath2GatewayFilter(securityGatewayFilter, getInfoService, mappingHelper, sensitiveComponent = sensitiveComponent)
    }

    /**
     * Retrieves the OAuth2 authentication component.
     *
     * @param webClient The WebClient used to make HTTP requests.
     * @param properties The IdentityServerProperties object containing the configuration properties for the identity server.
     * @return The Oath2AuthComponent object that handles OAuth2 authentication.
     */
    @Bean
    @ConditionalOnMissingBean(Oath2AuthComponent::class)
    @Lazy
//    @ConditionalOnBean(IdentityOath2GetUserInfoService::class)
    fun getOath2AuthComponent(
        webClient: WebClient,
        properties: IdentityServerProperties,
    ): Oath2AuthComponent {
        return Oath2AuthComponent(webClient, properties.uri!!, properties.clientId!!, properties.clientSecret!!, properties.clientScopes!!.split(" "), properties.grantType!!, properties.username, properties.password)
    }

    /**
     * Retrieves the OAuth2 user information component.
     *
     * @param mappingHelper The MappingHelper object for mapping user information.
     * @param webClient The WebClient used to make HTTP requests.
     * @param objectMapper The ObjectMapper for JSON serialization and deserialization.
     * @param properties The IdentityServerProperties object containing the configuration properties for the identity server.
     * @return The IdentityOath2GetUserInfoComponent instance.
     */
    @Bean
    @ConditionalOnMissingBean(IdentityOath2GetUserInfoComponent::class)
    @Lazy
//    @ConditionalOnBean(IdentityOath2GetUserInfoService::class)
    fun getOath2GetUserInfoComponent(
        mappingHelper: ObjectMapper,
        webClient: WebClient,
        objectMapper: ObjectMapper,
        properties: IdentityServerProperties,
    ): IdentityOath2GetUserInfoComponent {
        return IdentityOath2GetUserInfoComponent(mappingHelper, webClient, objectMapper, properties.uri!!)
    }
}
