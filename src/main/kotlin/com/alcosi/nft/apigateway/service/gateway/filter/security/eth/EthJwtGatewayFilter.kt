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

/**
 * EthJwtGatewayFilter is a subclass of JwtGatewayFilter that is specifically designed for handling
 * JSON Web Tokens (JWT) in an Ethereum context.
 *
 * @property securityGatewayFilter The SecurityGatewayFilter instance to be used for security checks.
 * @property checkJWTService The CheckJWTService instance to be used for JWT parsing and verification.
 * @property order The order of the filter in the chain (default is JWT_LOG_ORDER).
 * @property clientWalletHeader The name of the header used to pass the current wallet information (default is X-Client-Wallet).
 * @property clientWalletsHeader The name of the header used to pass the profile wallets information (default is X-Client-Wallets).
 * @property clientIdHeader The name of the header used to pass the profile ID information (default is X-Client-Id).
 */
open class EthJwtGatewayFilter(
    securityGatewayFilter: SecurityGatewayFilter,
    val checkJWTService: CheckJWTService,
    order: Int = JWT_LOG_ORDER,
    val clientWalletHeader: String = CLIENT_WALLET_HEADER,
    val clientWalletsHeader: String = CLIENT_WALLETS_HEADER,
    val clientIdHeader: String = CLIENT_ID_HEADER,
) : JwtGatewayFilter(securityGatewayFilter, listOf(clientIdHeader, clientWalletHeader, clientWalletsHeader), order) {
    /**
     * Mutates the given ServerWebExchange by adding client information from the provided JWT.
     *
     * @param jwt The JSON Web Token (JWT) string.
     * @param exchange The ServerWebExchange object to be mutated.
     * @param clientAttribute The attribute key to store the EthClient object in the exchange attributes.
     * @return A Mono containing the mutated ServerWebExchange.
     */
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

    /**
     * This companion object contains constant values that represent the headers used for clients in HTTP requests.
     *
     * The headers are:
     * 1. CLIENT_WALLET_HEADER: Represents the header name for a single client wallet.
     * 2. CLIENT_WALLETS_HEADER: Represents the header name for multiple client wallets.
     * 3. CLIENT_ID_HEADER: Represents the header name for the client ID.
     */
    companion object {
        val CLIENT_WALLET_HEADER: String = "X-Client-Wallet"
        val CLIENT_WALLETS_HEADER: String = "X-Client-Wallets"
        val CLIENT_ID_HEADER: String = "X-Client-Id"
    }
}
