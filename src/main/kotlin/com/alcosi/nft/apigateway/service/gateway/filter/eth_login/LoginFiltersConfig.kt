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

package com.alcosi.nft.apigateway.service.gateway.filter.eth_login

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService
import com.alcosi.nft.apigateway.auth.service.LoginRequestProcess
import com.alcosi.nft.apigateway.auth.service.NonceComponent
import com.alcosi.nft.apigateway.auth.service.RefreshTokenService
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.multi_wallet.BoundWalletsService
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
        @Value("\${gateway.base.path:/api}") basePath: String,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.auth.get.path.uri:/v1/auth/authority})(/){0,1}}") uriRegexString: String,
        @Value("\${spring.cloud.gateway.fake-uri:http://127.0.200.1:87787}") fakeUri: String,
    ): AuthoritiesGetGatewayFilter {
        val filter = AuthoritiesGetGatewayFilter(basePath, writer, uriRegexString, prepareHexService)
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(LoginGetGatewayFilter::class)
    fun getLoginFilter(
        @Value("\${gateway.base.path:/api}") basePath: String,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        nonceComponent: NonceComponent,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.login.get.path.uri:/v1/auth/login/})(?<hexprefix>0x){0,1}(?<wallet>[0-9a-fA-F]{40}\$)}") uriRegexString: String,
        @Value("\${spring.cloud.gateway.fake-uri:http://127.0.200.1:87787}") fakeUri: String,
        loginProcessors: List<LoginRequestProcess>,
    ): LoginGetGatewayFilter {
        val filter = LoginGetGatewayFilter(
            basePath,
            writer,
            prepareHexService,
            nonceComponent,
            uriRegexString,
            loginProcessors)
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(LoginPostGatewayFilter::class)
    fun postLoginFilter(
        @Value("\${gateway.base.path:/api}") basePath: String,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        nonceComponent: NonceComponent,
        refreshTokenService: RefreshTokenService,
        checkAuthSignatureService: com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.login.post.path.uri:/v1/auth/login/})(?<hexprefix>0x){0,1}(?<wallet>[0-9a-fA-F]{40}\$)}") uriRegexString : String,
        loginProcessors: List<LoginRequestProcess>,
    ): LoginPostGatewayFilter {
        val filter = LoginPostGatewayFilter(basePath,writer, prepareHexService,refreshTokenService, nonceComponent,checkAuthSignatureService,uriRegexString,loginProcessors)
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(LoginPutGatewayFilter::class)
    fun putLoginFilter(
        @Value("\${gateway.base.path:/api}") basePath: String,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        refreshTokenService: RefreshTokenService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.login.put.path.uri:/v1/auth/login/})(?<hexprefix>0x){0,1}(?<wallet>[0-9a-fA-F]{40}\$)}") uriRegexString : String ,
        loginProcessors: List<LoginRequestProcess>,
    ): LoginPutGatewayFilter {
        val filter = LoginPutGatewayFilter(basePath,writer, prepareHexService,refreshTokenService,uriRegexString,loginProcessors)
        return filter
    }

    @Bean
    @ConditionalOnMissingBean(AuthBoundWalletsPutGatewayFilter::class)
    @ConditionalOnBean(BoundWalletsService::class)
    fun putBoundWalletsFilter(
        @Value("\${gateway.base.path:/api}") basePath: String,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.bound.put.path.uri:/v1/auth/bound/})(?<profileid>[0-9]{0,40})(/)(?<hexprefix>0x){0,1}(?<walletsecond>[0-9a-fA-F]{40}\$)}") uriRegexString : String,
        boundWalletsService: BoundWalletsService,
        mappingHelper: MappingHelper,
        nonceComponent: NonceComponent,
        checkSignatureService: CheckAuthSignatureService,
         refreshTokenService: RefreshTokenService,
        ): AuthBoundWalletsPutGatewayFilter {
        val filter = AuthBoundWalletsPutGatewayFilter(basePath,writer,uriRegexString,prepareHexService,mappingHelper,boundWalletsService,nonceComponent,checkSignatureService,refreshTokenService,SecurityGatewayFilter.SECURITY_CLIENT_ATTRIBUTE)
        return filter
    }
}