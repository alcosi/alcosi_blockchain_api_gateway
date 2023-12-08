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

package com.alcosi.nft.apigateway.service.multi_wallet

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.lib.utils.PrepareHexService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.SearchStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient

@Configuration

class MultiWalletConfig {
    @ConditionalOnProperty(prefix = "auth.multi-wallet", name = ["provider"], matchIfMissing = false, havingValue = "http-service")
    @Bean
    @Primary
    fun getHttpServiceWalletMultiWalletProvider(
        prepareHexService: PrepareHexService,
        webClient: WebClient,
        @Value("\${auth.multi-wallet.http-service.service-address:\${gateway.microservice.uri.profile}/profile/wallet/}") serviceAddress:String,
        @Value("\${auth.multi-wallet.http-service.service-method:GET}") method:HttpMethod,
    ):MultiWalletProvider{
        return HttpServiceMultiWalletProvider(prepareHexService,webClient,serviceAddress,method)
    }
    @ConditionalOnMissingBean(value = [MultiWalletProvider::class], search = SearchStrategy.CURRENT)
    @Bean
    fun getSingleWalletMultiWalletProvider(prepareHexService: PrepareHexService):MultiWalletProvider{
        return SingleMultiWalletProvider(prepareHexService)
    }

    @ConditionalOnMissingBean(value = [BoundWalletsService::class], search = SearchStrategy.CURRENT)
    @Bean
    @ConditionalOnProperty(prefix = "auth.multi-wallet", name = ["provider"], matchIfMissing = false, havingValue = "http-service")
    fun getDefaultBoundWalletsService(
       webClient: WebClient,
       @Value("\${gateway.microservice.uri.profile:http://127.0.0.1:8080/profile}\${auth.multi-wallet.bound.service.address:/profile/bound/{profileId}/{walletSecond}}") serviceUriTemplate: String,
       @Value("\${auth.multi-wallet.bound.service.method:POST}")  method:HttpMethod,
       mappingHelper: MappingHelper
    ):BoundWalletsService{
        return DefaultBoundWalletsService(webClient, serviceUriTemplate, method, mappingHelper)
    }
}