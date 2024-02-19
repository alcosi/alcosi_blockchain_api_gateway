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
    protected open val emptyNode = jsonMapper().createObjectNode()
    override fun mutateExchange(
        jwt: String,
        exchange: ServerWebExchange,
        clientAttribute: String,
    ): Mono<ServerWebExchange> {
        val claims = checkJWTService.parse(jwt)
        val currentWallet = claims.get("currentWallet", String::class.java)
        val profileWallets = claims.get("profileWallets", List::class.java) as List<String>
        val profileId = claims.get("profileId", String::class.java)
        exchange.attributes[clientAttribute] = EthClient(currentWallet, profileWallets, profileId, emptyNode)
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
