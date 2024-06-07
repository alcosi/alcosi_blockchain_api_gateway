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
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Represents a provider for retrieving wallet configurations from an HTTP service.
 *
 * @param hexService The instance of PrepareHexService used for preparing wallet addresses.
 * @param webClient The instance of WebClient used for making HTTP requests.
 * @param serviceAddress The address of the HTTP service.
 * @param httpMethod The HTTP method to be used for the request.
 */
open class HttpServiceMultiWalletProvider(
    protected val hexService: PrepareHexService,
    protected val webClient: WebClient,
    protected val serviceAddress: String,
    protected val httpMethod: HttpMethod,
) : MultiWalletProvider {
    /**
     * Retrieves the list of wallet configurations for a specific wallet.
     *
     * @param wallet The wallet identifier.
     * @return A Mono that emits the MultiWalletConfig object containing the list of wallets and the profile ID.
     */
    override fun getWalletsListByWallet(wallet: String): Mono<MultiWalletProvider.MultiWalletConfig> {
        val rqUri = serviceAddress + hexService.prepareAddr(wallet)
        val httpRs =
            webClient
                .method(httpMethod)
                .uri(rqUri)
                .retrieve()
        return httpRs.bodyToMono(MultiWalletProvider.MultiWalletConfig::class.java)
    }
}
