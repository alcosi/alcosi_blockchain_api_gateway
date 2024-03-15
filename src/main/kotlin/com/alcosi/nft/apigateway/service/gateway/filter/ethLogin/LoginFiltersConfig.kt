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

package com.alcosi.nft.apigateway.service.gateway.filter.ethLogin

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService
import com.alcosi.nft.apigateway.auth.service.LoginRequestProcess
import com.alcosi.nft.apigateway.auth.service.NonceComponent
import com.alcosi.nft.apigateway.auth.service.RefreshTokenService
import com.alcosi.nft.apigateway.service.gateway.GatewayBasePathProperties
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.multiWallet.BoundWalletsService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
class LoginFiltersConfig {
    @Bean
    @ConditionalOnMissingBean(AuthoritiesGetGatewayFilter::class)
    fun getAuthoritiesFilter(
        gatewayBasePathProperties: GatewayBasePathProperties,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.auth.get.path.uri:/v1/auth/authority})(/){0,1}}") uriRegexString: String,
    ): AuthoritiesGetGatewayFilter {
        val filter = AuthoritiesGetGatewayFilter(gatewayBasePathProperties.path, writer, uriRegexString, prepareHexService)
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(LoginGetGatewayFilter::class)
    fun getLoginFilter(
        gatewayBasePathProperties: GatewayBasePathProperties,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        nonceComponent: NonceComponent,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.login.get.path.uri:/v1/auth/login/})(?<hexprefix>0x){0,1}(?<wallet>[0-9a-fA-F]{40}\$)}") uriRegexString: String,
        loginProcessors: List<LoginRequestProcess>,
    ): LoginGetGatewayFilter {
        val filter =
            LoginGetGatewayFilter(
                gatewayBasePathProperties.path,
                writer,
                prepareHexService,
                nonceComponent,
                uriRegexString,
                loginProcessors,
            )
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(LoginPostGatewayFilter::class)
    fun postLoginFilter(
        gatewayBasePathProperties: GatewayBasePathProperties,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        nonceComponent: NonceComponent,
        refreshTokenService: RefreshTokenService,
        checkAuthSignatureService: com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.login.post.path.uri:/v1/auth/login/})(?<hexprefix>0x){0,1}(?<wallet>[0-9a-fA-F]{40}\$)}") uriRegexString: String,
        loginProcessors: List<LoginRequestProcess>,
    ): LoginPostGatewayFilter {
        val filter = LoginPostGatewayFilter(gatewayBasePathProperties.path, writer, prepareHexService, refreshTokenService, nonceComponent, checkAuthSignatureService, uriRegexString, loginProcessors)
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(LoginPutGatewayFilter::class)
    fun putLoginFilter(
        @Value("\${gateway.base.path:/api}") basePath: String,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        refreshTokenService: RefreshTokenService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.login.put.path.uri:/v1/auth/login/})(?<hexprefix>0x){0,1}(?<wallet>[0-9a-fA-F]{40}\$)}") uriRegexString: String,
        loginProcessors: List<LoginRequestProcess>,
    ): LoginPutGatewayFilter {
        val filter = LoginPutGatewayFilter(basePath, writer, prepareHexService, refreshTokenService, uriRegexString, loginProcessors)
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(AuthBoundWalletsPutGatewayFilter::class)
    @ConditionalOnBean(BoundWalletsService::class)
    fun putBoundWalletsFilter(
        gatewayBasePathProperties: GatewayBasePathProperties,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.bound.put.path.uri:/v1/auth/bound/})(?<profileid>[0-9]{0,40})(/)(?<hexprefix>0x){0,1}(?<walletsecond>[0-9a-fA-F]{40}\$)}") uriRegexString: String,
        boundWalletsService: BoundWalletsService,
        mappingHelper: MappingHelper,
        nonceComponent: NonceComponent,
        checkSignatureService: CheckAuthSignatureService,
        refreshTokenService: RefreshTokenService,
    ): AuthBoundWalletsPutGatewayFilter {
        val filter =
            AuthBoundWalletsPutGatewayFilter(
                gatewayBasePathProperties.path,
                writer,
                uriRegexString,
                prepareHexService,
                mappingHelper,
                boundWalletsService,
                nonceComponent,
                checkSignatureService,
                refreshTokenService,
                SecurityGatewayFilter.SECURITY_CLIENT_ATTRIBUTE,
            )
        return filter
    }
}
