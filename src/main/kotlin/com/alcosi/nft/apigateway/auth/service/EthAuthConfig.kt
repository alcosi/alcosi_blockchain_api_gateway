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

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.db.NonceDBComponent
import com.alcosi.nft.apigateway.auth.service.db.RefreshTokenDBComponent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
@Configuration
class EthAuthConfig {
    @Bean
    @ConditionalOnMissingBean(NonceDBComponent::class)
    @ConditionalOnBean(ReactiveStringRedisTemplate::class)
    fun getNonceDBComponent(
        redisTemplate: ReactiveStringRedisTemplate,
        mappingHelper: MappingHelper,
        @Value("\${jwt.nonce.lifetime:5m}") lifetime: Duration,
        @Value("\${jwt.nonce.redis_prefix:LOGIN_NONCE}") keyPrefix: String,
    ): NonceDBComponent {
        return NonceDBComponent(redisTemplate, mappingHelper, lifetime, keyPrefix)
    }
    @Bean
    @ConditionalOnMissingBean(RefreshTokenDBComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getRefreshTokenDBComponent(
        redisTemplate: ReactiveStringRedisTemplate,
        mappingHelper: MappingHelper,
        @Value("\${jwt.rt.lifetime:7d}") rtLifetime: Duration
    ): RefreshTokenDBComponent {
        return RefreshTokenDBComponent(redisTemplate, mappingHelper, rtLifetime)
    }

    @Bean
    @ConditionalOnMissingBean(PrepareLoginMsgComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getPrepareLoginMsgComponent(
        @Value("\${message.login:Please connect your wallet \n  @nonce@}")
         loginTemplate:String,
    ): PrepareLoginMsgComponent {
        return PrepareLoginMsgComponent(loginTemplate)
    }
    @Bean
    @ConditionalOnMissingBean(NonceComponent::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getNonceComponent(
        prepareArgsService: PrepareHexService,
        prepareMsgComponent: PrepareLoginMsgComponent,
        nonceDBComponent: NonceDBComponent,
        @Value("\${jwt.nonce.lifetime:5m}")  lifetime: Duration,
    ): NonceComponent {
        return NonceComponent(prepareArgsService, prepareMsgComponent, nonceDBComponent, lifetime)
    }

    @Bean
    @ConditionalOnMissingBean(LoginRequestProcess::class)
    @ConditionalOnProperty(
        matchIfMissing = true,
        prefix = "gateway.defaultRequestLoginRequestProcess",
        value = ["enabled"],
        havingValue = "true"
    )
    fun getDefaultRequestLoginRequestProcess(
        @Value("\${gateway.microservice.uri.DefaultRequestLoginRequestProcess}")  serviceUri: String,
        @Value("\${gateway.defaultRequestLoginRequestProcess.rqTypes:}") rqTypesString: String,
        @Value("\${gateway.defaultRequestLoginRequestProcess.types:}") typesString: String,
        @Value("\${gateway.defaultRequestLoginRequestProcess.method:POST}")  method: HttpMethod,
        webClient: WebClient,
        mappingHelper: MappingHelper,
        ): DefaultRequestLoginRequestProcess {
        return DefaultRequestLoginRequestProcess(serviceUri, rqTypesString, typesString, method, webClient, mappingHelper)
    }

    @Bean
    @ConditionalOnMissingBean(CheckAuthSignatureService::class)
    @ConditionalOnBean(NonceDBComponent::class)
    fun getCheckAuthSignatureService(
        @Value("\${check.sign.disable:false}") disable: Boolean,
        prepareArgsService: PrepareHexService
    ): CheckAuthSignatureService {
        return CheckAuthSignatureService(disable, prepareArgsService)
    }
    @Bean
    @ConditionalOnMissingBean(RefreshTokenService::class)
    @ConditionalOnBean(value = [CreateJWTService::class,RefreshTokenDBComponent::class,CheckJWTService::class])
    fun getRefreshTokenService(
         createJWTService: CreateJWTService,
         dbComponent: RefreshTokenDBComponent,
         checkJWTService: CheckJWTService,
    ): RefreshTokenService {
        return RefreshTokenService(createJWTService, dbComponent, checkJWTService)
    }

}