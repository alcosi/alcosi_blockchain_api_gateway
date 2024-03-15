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

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.db.NonceDBComponent
import com.alcosi.nft.apigateway.auth.service.db.RefreshTokenDBComponent
import com.alcosi.nft.apigateway.config.path.SecurityRoutesProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.web.reactive.function.client.WebClient

@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(EthJwtProperties::class, EthJwtLoginProcessorProperties::class, SecurityRoutesProperties::class)
class EthAuthConfig {
    @Bean
    @ConditionalOnMissingBean(NonceDBComponent::class)
    @ConditionalOnBean(ReactiveStringRedisTemplate::class)
    fun getNonceDBComponent(
        redisTemplate: ReactiveStringRedisTemplate,
        mappingHelper: MappingHelper,
        properties: EthJwtProperties,
    ): NonceDBComponent {
        return NonceDBComponent(redisTemplate, mappingHelper, properties.nonce.lifetime, properties.nonce.redisPrefix)
    }

    @Bean
    @ConditionalOnMissingBean(RefreshTokenDBComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getRefreshTokenDBComponent(
        redisTemplate: ReactiveStringRedisTemplate,
        mappingHelper: MappingHelper,
        properties: EthJwtProperties,
    ): RefreshTokenDBComponent {
        return RefreshTokenDBComponent(redisTemplate, mappingHelper, properties.token.rtLifetime)
    }

    @Bean
    @ConditionalOnMissingBean(PrepareLoginMsgComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getPrepareLoginMsgComponent(properties: EthJwtProperties): PrepareLoginMsgComponent {
        return PrepareLoginMsgComponent(properties.loginTemplate)
    }

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
        mappingHelper: MappingHelper,
    ): DefaultRequestLoginRequestProcess {
        return DefaultRequestLoginRequestProcess(properties.serviceUri, properties.rqTypes, properties.types, properties.serviceMethod, webClient, mappingHelper)
    }

    @Bean
    @ConditionalOnMissingBean(CheckAuthSignatureService::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getCheckAuthSignatureService(
        properties: EthJwtProperties,
        prepareArgsService: PrepareHexService,
    ): CheckAuthSignatureService {
        return CheckAuthSignatureService(properties.checkSignDisable, prepareArgsService)
    }

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
