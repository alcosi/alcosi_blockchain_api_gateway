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
