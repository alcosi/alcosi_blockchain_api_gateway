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

package com.alcosi.nft.apigateway.auth.service

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.db.NonceDBComponent
import com.alcosi.nft.apigateway.auth.service.db.RefreshTokenDBComponent
import com.alcosi.nft.apigateway.config.path.SecurityRoutesProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration class for EthAuth.
 *
 * This class provides beans for various components used by EthAuth.
 * It is conditionally enabled based on the application property `filter.config.path.security.type.method`.
 * It supports the following properties:
 * - `method`: the authentication method, should be set to "ETH_JWT"
 *
 * This class requires the following properties to be enabled:
 * - EthJwtProperties: configuration properties for Eth JWT
 * - EthJwtLoginProcessorProperties: configuration properties for Eth JWT login processor
 * - SecurityRoutesProperties: configuration properties for security routes
 *
 * @property EthJwtProperties Eth JWT configuration properties
 * @property EthJwtLoginProcessorProperties Eth JWT login processor configuration properties
 * @property SecurityRoutesProperties Security routes configuration properties
 */
@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(EthJwtProperties::class, EthJwtLoginProcessorProperties::class, SecurityRoutesProperties::class)
class EthAuthConfig {
    /**
     * Retrieves the NonceDBComponent instance.
     *
     * @param redisTemplate The Redis template for interacting with the Redis server.
     * @param mappingHelper The helper class for mapping objects.
     * @param properties The properties related to EthJwt.
     * @return The NonceDBComponent instance.
     */
    @Bean
    @ConditionalOnMissingBean(NonceDBComponent::class)
    @ConditionalOnBean(ReactiveStringRedisTemplate::class)
    fun getNonceDBComponent(
        redisTemplate: ReactiveStringRedisTemplate,
        mappingHelper: ObjectMapper,
        properties: EthJwtProperties,
    ): NonceDBComponent {
        return NonceDBComponent(redisTemplate, mappingHelper, properties.nonce.lifetime, properties.nonce.redisPrefix)
    }

    /**
     * Retrieves the RefreshTokenDBComponent instance.
     *
     * @param redisTemplate The Redis template for interacting with the Redis server.
     * @param mappingHelper The helper class for mapping objects.
     * @param properties The properties related to EthJwt.
     * @return The RefreshTokenDBComponent instance.
     */
    @Bean
    @ConditionalOnMissingBean(RefreshTokenDBComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getRefreshTokenDBComponent(
        redisTemplate: ReactiveStringRedisTemplate,
        mappingHelper: ObjectMapper,
        properties: EthJwtProperties,
    ): RefreshTokenDBComponent {
        return RefreshTokenDBComponent(redisTemplate, mappingHelper, properties.token.rtLifetime)
    }

    /**
     * Retrieves the PrepareLoginMsgComponent instance.
     *
     * @param properties The properties related to EthJwt.
     * @return The PrepareLoginMsgComponent instance.
     */
    @Bean
    @ConditionalOnMissingBean(PrepareLoginMsgComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getPrepareLoginMsgComponent(properties: EthJwtProperties): PrepareLoginMsgComponent {
        return PrepareLoginMsgComponent(properties.loginTemplate)
    }

    /**
     * Retrieves the NonceComponent instance.
     *
     * @param prepareArgsService The service for preparing hexadecimal arguments.
     * @param prepareMsgComponent The component for preparing login messages.
     * @param nonceDBComponent The NonceDBComponent instance.
     * @param properties The EthJwtProperties instance.
     * @return The NonceComponent instance.
     */
    @Bean
    @ConditionalOnMissingBean(NonceComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getNonceComponent(
        prepareArgsService: PrepareHexService,
        prepareMsgComponent: PrepareLoginMsgComponent,
        nonceDBComponent: NonceDBComponent,
        properties: EthJwtProperties,
    ): NonceComponent {
        return NonceComponent(prepareArgsService, prepareMsgComponent, nonceDBComponent, properties.nonce.lifetime)
    }

    /**
     * Retrieves the DefaultRequestLoginRequestProcess instance.
     *
     * @param properties The properties related to EthJwtLoginProcessor.
     * @param webClient The WebClient instance.
     * @param mappingHelper The MappingHelper instance.
     * @return The DefaultRequestLoginRequestProcess instance.
     */
    @Bean
    @ConditionalOnMissingBean(LoginRequestProcess::class)
    @ConditionalOnProperty(
        matchIfMissing = true,
        prefix = "gateway.default-request-login-request-process",
        value = ["enabled"],
        havingValue = "true",
    )
    fun getDefaultRequestLoginRequestProcess(
        properties: EthJwtLoginProcessorProperties,
        webClient: WebClient,
        mappingHelper: ObjectMapper,
    ): DefaultRequestLoginRequestProcess {
        return DefaultRequestLoginRequestProcess(properties.serviceUri!!, properties.rqTypes, properties.types, properties.serviceMethod!!, webClient, mappingHelper)
    }

    /**
     * Retrieves the CheckAuthSignatureService instance.
     *
     * @param properties The EthJwtProperties instance.
     * @param prepareArgsService The PrepareHexService instance.
     * @return The CheckAuthSignatureService instance.
     */
    @Bean
    @ConditionalOnMissingBean(CheckAuthSignatureService::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getCheckAuthSignatureService(
        properties: EthJwtProperties,
        prepareArgsService: PrepareHexService,
    ): CheckAuthSignatureService {
        return CheckAuthSignatureService(properties.checkSignDisable, prepareArgsService)
    }

    /**
     * Returns an instance of RefreshTokenService.
     *
     * @param createJWTService The CreateJWTService instance used for creating JSON Web Tokens.
     * @param dbComponent The RefreshTokenDBComponent instance used for interacting with the Redis server.
     * @param checkJWTService The CheckJWTService instance used for checking the validity of JWT.
     * @return An instance of RefreshTokenService.
     */
    @Bean
    @ConditionalOnMissingBean(RefreshTokenService::class)
    @ConditionalOnBean(value = [CreateJWTService::class, RefreshTokenDBComponent::class, CheckJWTService::class])
    fun getRefreshTokenService(
        createJWTService: CreateJWTService,
        dbComponent: RefreshTokenDBComponent,
        checkJWTService: CheckJWTService,
    ): RefreshTokenService {
        return RefreshTokenService(createJWTService, dbComponent, checkJWTService)
    }
}
