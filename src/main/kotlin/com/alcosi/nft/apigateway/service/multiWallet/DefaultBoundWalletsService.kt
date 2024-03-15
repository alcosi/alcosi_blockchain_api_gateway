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

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.auth.dto.EthClient
import com.alcosi.nft.apigateway.service.gateway.filter.security.eth.EthJwtGatewayFilter.Companion.CLIENT_ID_HEADER
import com.alcosi.nft.apigateway.service.gateway.filter.security.eth.EthJwtGatewayFilter.Companion.CLIENT_WALLETS_HEADER
import com.alcosi.nft.apigateway.service.gateway.filter.security.eth.EthJwtGatewayFilter.Companion.CLIENT_WALLET_HEADER
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

open class DefaultBoundWalletsService(
    protected val webClient: WebClient,
    protected val serviceUriTemplate: String,
    protected val method: HttpMethod,
    protected val mappingHelper: MappingHelper,
) : BoundWalletsService {
    @JvmRecord
    data class AddRs(val status: String)

    override fun bound(
        client: EthClient,
        walletSecond: String,
    ): Mono<String> {
        val uri =
            serviceUriTemplate
                .replace("{profileId}", client.profileId)
                .replace("{walletSecond}", walletSecond)
        return webClient
            .method(method)
            .uri(uri)
            .header(CLIENT_WALLET_HEADER, client.currentWallet)
            .header(CLIENT_WALLETS_HEADER, (client.profileWallets + listOf(walletSecond)).joinToString(","))
            .header(CLIENT_ID_HEADER, client.profileId)
            .retrieve()
            .bodyToMono(AddRs::class.java)
            .map { it.status }
    }
}
