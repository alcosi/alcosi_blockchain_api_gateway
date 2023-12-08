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
import com.alcosi.nft.apigateway.auth.dto.SecurityClient
import com.alcosi.nft.apigateway.service.gateway.filter.security.X_CLIENT_ID_HEADER
import com.alcosi.nft.apigateway.service.gateway.filter.security.X_CLIENT_WALLET_HEADER
import com.alcosi.nft.apigateway.service.gateway.filter.security.X_CLIENT_WALLET_LIST_HEADER
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class DefaultBoundWalletsService(
    val webClient: WebClient,
    val serviceUriTemplate: String,
    val method:HttpMethod,
    val mappingHelper: MappingHelper
) : BoundWalletsService {
    @JvmRecord
    data class AddRs(val status:String) {
    }
    override fun bound(client: SecurityClient, walletSecond: String): Mono<String> {
        val uri=serviceUriTemplate
            .replace("{profileId}",client.profileId)
            .replace("{walletSecond}",walletSecond);
        return webClient
            .method(method)
            .uri(uri)
            .header(X_CLIENT_WALLET_HEADER,client.currentWallet)
            .header(X_CLIENT_WALLET_LIST_HEADER,(client.profileWallets+listOf(walletSecond)).joinToString(","))
            .header(X_CLIENT_ID_HEADER,client.profileId)
            .retrieve()
            .bodyToMono(AddRs::class.java)
            .map {it.status }


    }
}