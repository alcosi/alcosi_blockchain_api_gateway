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

package com.alcosi.nft.apigateway.service.multiWallet

import com.alcosi.lib.utils.PrepareHexService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.SearchStrategy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient

/**
 * Represents the configuration class for the MultiWallet feature.
 */
@ConditionalOnProperty(prefix = "auth.multi-wallet", name = ["disabled"], matchIfMissing = true, havingValue = "false")
@Configuration
@EnableConfigurationProperties(MultiWalletProperties::class)
class MultiWalletConfig {
    /**
     * Retrieves the HTTP service wallet multi-wallet provider.
     *
     * @param prepareHexService The instance of PrepareHexService used for preparing wallet addresses.
     * @param webClient The instance of WebClient used for making HTTP requests.
     * @param properties The MultiWalletProperties object containing the configuration properties.
     * @return The MultiWalletProvider implementation that retrieves wallet configurations from an HTTP service.
     */
    @ConditionalOnProperty(prefix = "auth.multi-wallet", name = ["provider"], matchIfMissing = false, havingValue = "HTTP_SERVICE")
    @Bean
    @Primary
    fun getHttpServiceWalletMultiWalletProvider(
        prepareHexService: PrepareHexService,
        webClient: WebClient,
        properties: MultiWalletProperties,
    ): MultiWalletProvider {
        return HttpServiceMultiWalletProvider(prepareHexService, webClient, properties.httpService.uri, properties.httpService.method)
    }

    /**
     * Retrieves a single wallet multi-wallet provider.
     *
     * @param prepareHexService The instance of PrepareHexService used for preparing wallet addresses.
     * @return The MultiWalletProvider implementation that handles a single wallet.
     */
    @ConditionalOnMissingBean(value = [MultiWalletProvider::class], search = SearchStrategy.CURRENT)
    @Bean
    fun getSingleWalletMultiWalletProvider(prepareHexService: PrepareHexService): MultiWalletProvider {
        return SingleMultiWalletProvider(prepareHexService)
    }

    /**
     * Retrieves the default implementation of the BoundWalletsService interface.
     *
     * @param webClient The instance of WebClient used for making HTTP requests.
     * @param properties The MultiWalletProperties object containing the configuration properties.
     * @param mappingHelper The instance of MappingHelper used for mapping responses.
     * @return The default implementation of BoundWalletsService.
     */
    @ConditionalOnMissingBean(value = [BoundWalletsService::class], search = SearchStrategy.CURRENT)
    @Bean
    @ConditionalOnProperty(prefix = "auth.multi-wallet", name = ["provider"], matchIfMissing = false, havingValue = "HTTP_SERVICE")
    fun getDefaultBoundWalletsService(
        webClient: WebClient,
        properties: MultiWalletProperties,
        mappingHelper: ObjectMapper,
    ): BoundWalletsService {
        return DefaultBoundWalletsService(webClient, properties.bound.uri, properties.bound.method, mappingHelper)
    }
}
