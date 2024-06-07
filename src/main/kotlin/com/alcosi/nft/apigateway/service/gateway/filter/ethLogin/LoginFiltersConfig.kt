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

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.CheckAuthSignatureService
import com.alcosi.nft.apigateway.auth.service.LoginRequestProcess
import com.alcosi.nft.apigateway.auth.service.NonceComponent
import com.alcosi.nft.apigateway.auth.service.RefreshTokenService
import com.alcosi.nft.apigateway.service.gateway.GatewayBasePathProperties
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.multiWallet.BoundWalletsService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * LoginFiltersConfig is a configuration class that defines various filters
 * for the login process. It is annotated with @Configuration to indicate
 * that it is a configuration class. It is conditionally enabled based on
 * the property "filter.config.path.security.type.method" with a default
 * value of "ETH_JWT".
 */
@Configuration
@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
class LoginFiltersConfig {
    /**
     * Retrieves a filter for handling GET requests and retrieving authorities
     * from a client.
     *
     * @param gatewayBasePathProperties The GatewayBasePathProperties object
     *     containing the gateway base path.
     * @param writer The GatewayFilterResponseWriter used to write the
     *     response.
     * @param prepareHexService The PrepareHexService used to prepare the
     *     wallet address.
     * @param properties EthLoginProperties.
     * @return An instance of AuthoritiesGetGatewayFilter.
     */
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

    /**
     * Retrieves a filter for handling GET requests and retrieving authorities
     * from a client.
     *
     * @param gatewayBasePathProperties The GatewayBasePathProperties object
     *     containing the gateway base path.
     * @param writer The GatewayFilterResponseWriter used to write the
     *     response.
     * @param prepareHexService The PrepareHexService used to prepare the
     *     wallet address.
     * @param nonceComponent The NonceComponent used to get a new nonce.
     * @param uriRegexString The regular expression to match against the
     *     request URI.
     * @param loginProcessors The list of LoginRequestProcess objects.
     * @return An instance of LoginGetGatewayFilter.
     */
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

    /**
     * Returns an instance of LoginPostGatewayFilter.
     *
     * @param gatewayBasePathProperties The GatewayBasePathProperties object
     *     containing the gateway base path.
     * @param writer The GatewayFilterResponseWriter used to write the
     *     response.
     * @param prepareHexService The PrepareHexService used to prepare the
     *     wallet address.
     * @param nonceComponent The NonceComponent used to get a new nonce.
     * @param refreshTokenService The RefreshTokenService used to save token
     *     information.
     * @param checkAuthSignatureService The CheckAuthSignatureService used to
     *     check the signature.
     * @param uriRegexString The regular expression to match against the
     *     request URI.
     * @param loginProcessors The list of LoginRequestProcess objects.
     * @return An instance of LoginPostGatewayFilter.
     */
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

    /**
     * Creates and returns an instance of `LoginPutGatewayFilter`.
     *
     * @param basePath The base path for the gateway.
     * @param writer The GatewayFilterResponseWriter used to write the
     *     response.
     * @param prepareHexService The PrepareHexService used to prepare the
     *     wallet address.
     * @param refreshTokenService The RefreshTokenService used to save token
     *     information.
     * @param uriRegexString The regular expression to match against the
     *     request URI.
     * @param loginProcessors The list of LoginRequestProcess objects.
     * @return An instance of LoginPutGatewayFilter.
     */
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

    /**
     * Creates and returns an instance of `AuthBoundWalletsPutGatewayFilter`.
     *
     * @param gatewayBasePathProperties The GatewayBasePathProperties object
     *     containing the gateway base path.
     * @param writer The GatewayFilterResponseWriter used to write the
     *     response.
     * @param prepareHexService The PrepareHexService used to prepare the
     *     wallet address.
     * @param uriRegexString The regular expression to match against the
     *     request URI.
     * @param boundWalletsService The BoundWalletsService used to handle bound
     *     wallet operations.
     * @param mappingHelper The MappingHelper used for mapping objects.
     * @param nonceComponent The NonceComponent used to get a new nonce.
     * @param checkSignatureService The CheckAuthSignatureService used to check
     *     the signature.
     * @param refreshTokenService The RefreshTokenService used to save token
     *     information.
     * @return An instance of AuthBoundWalletsPutGatewayFilter.
     */
    @Bean
    @ConditionalOnMissingBean(AuthBoundWalletsPutGatewayFilter::class)
    @ConditionalOnBean(BoundWalletsService::class)
    fun putBoundWalletsFilter(
        gatewayBasePathProperties: GatewayBasePathProperties,
        writer: GatewayFilterResponseWriter,
        prepareHexService: PrepareHexService,
        @Value("\${gateway.auth.get.path.regex:(?<uri>\${gateway.base.path:/api}\${gateway.bound.put.path.uri:/v1/auth/bound/})(?<profileid>[0-9]{0,40})(/)(?<hexprefix>0x){0,1}(?<walletsecond>[0-9a-fA-F]{40}\$)}") uriRegexString: String,
        boundWalletsService: BoundWalletsService,
        mappingHelper: ObjectMapper,
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
