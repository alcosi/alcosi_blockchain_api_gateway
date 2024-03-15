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

package com.alcosi.nft.apigateway.service.gateway.filter.security.eth

import com.alcosi.nft.apigateway.auth.dto.EthClient
import com.alcosi.nft.apigateway.auth.service.CheckJWTService
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.fasterxml.jackson.module.kotlin.jsonMapper
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

open class EthJwtGatewayFilter(
    securityGatewayFilter: SecurityGatewayFilter,
    val checkJWTService: CheckJWTService,
    order: Int = JWT_LOG_ORDER,
    val clientWalletHeader: String = CLIENT_WALLET_HEADER,
    val clientWalletsHeader: String = CLIENT_WALLETS_HEADER,
    val clientIdHeader: String = CLIENT_ID_HEADER,
) : JwtGatewayFilter(securityGatewayFilter, listOf(clientIdHeader, clientWalletHeader, clientWalletsHeader), order) {
    override fun mutateExchange(
        jwt: String,
        exchange: ServerWebExchange,
        clientAttribute: String,
    ): Mono<ServerWebExchange> {
        val claims = checkJWTService.parse(jwt)
        val currentWallet = claims.get("currentWallet", String::class.java)
        val profileWallets = claims.get("profileWallets", List::class.java) as List<String>
        val profileId = claims.get("profileId", String::class.java)
        exchange.attributes[clientAttribute] = EthClient(currentWallet, profileWallets, profileId)
        val withWallet =
            exchange.request
                .mutate()
                .header(clientWalletHeader, currentWallet)
                .header(clientWalletsHeader, profileWallets.joinToString(","))
                .header(clientIdHeader, profileId)
                .build()
        return Mono.just(exchange.mutate().request(withWallet).build())
    }

    companion object {
        val CLIENT_WALLET_HEADER: String = "X-Client-Wallet"
        val CLIENT_WALLETS_HEADER: String = "X-Client-Wallets"
        val CLIENT_ID_HEADER: String = "X-Client-Id"
    }
}
