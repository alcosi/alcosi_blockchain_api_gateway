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

import com.alcosi.nft.apigateway.service.multiWallet.MultiWalletProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for JWT Service.
 */
@Configuration
@EnableConfigurationProperties(EthJwtProperties::class)
@ConditionalOnProperty(
    prefix = "filter.config.path.security.type",
    name = ["method"],
    havingValue = "ETH_JWT",
    matchIfMissing = true,
)
class JWTServiceConfig {
    /**
     * Retrieves an instance of the CheckJWTService.
     *
     * @param properties The EthJwtProperties object containing the configuration properties for EthJwt.
     * @return An instance of the CheckJWTService initialized with the private key from the EthJwtProperties.
     * @see CheckJWTService
     */
    @Bean
    @ConditionalOnMissingBean(CheckJWTService::class)
    fun getCheckJWTService(properties: EthJwtProperties): CheckJWTService {
        return CheckJWTService(properties.key.privateKey!!)
    }

    /**
     * Retrieves an instance of the CreateJWTService.
     *
     * @param properties The EthJwtProperties object containing the configuration properties for EthJwt.
     * @param multiWalletProvider The MultiWalletProvider object for retrieving wallet information.
     * @return An instance of the CreateJWTService initialized with the specified properties.
     * @see CreateJWTService
     */
    @Bean
    @ConditionalOnMissingBean(CreateJWTService::class)
    fun getCreateJWTService(
        properties: EthJwtProperties,
        multiWalletProvider: MultiWalletProvider,
    ): CreateJWTService {
        return CreateJWTService(
            properties.token.lifetime,
            properties.token.issuer,
            properties.key.private!!,
            multiWalletProvider,
        )
    }
}
