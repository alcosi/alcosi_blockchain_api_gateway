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

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.error.ErrorRs
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.alcosi.nft.apigateway.service.gateway.filter.security.eth.EthJwtGatewayFilter
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

open class DefaultRequestLoginRequestProcess(
    val serviceUri: String,
    val rqTypes: List<LoginRequestProcess.RequestType>,
    val types: List<LoginRequestProcess.TYPE>,
    val method: HttpMethod,
    val webClient: WebClient,
    val mappingHelper: MappingHelper,
) : Logging, LoginRequestProcess {
    open val clientWalletHeader: String = EthJwtGatewayFilter.CLIENT_WALLET_HEADER
    open val clientWalletsHeader: String = EthJwtGatewayFilter.CLIENT_WALLETS_HEADER

    override fun rqTypes(): List<LoginRequestProcess.RequestType> {
        return rqTypes
    }

    override fun types(): List<LoginRequestProcess.TYPE> {
        return types
    }

    override fun method(): HttpMethod {
        return method
    }

    override fun process(wallet: String): Mono<Void> {
        val uri = "$serviceUri/$wallet"
        val response =
            webClient
                .method(method())
                .uri(uri)
                .header(clientWalletHeader, wallet)
                .header(clientWalletsHeader, listOf(wallet).joinToString(","))
                .exchangeToMono { res ->
                    val t =
                        res.body { inputMessage, _ ->
                            val msg =
                                DataBufferUtils.join(inputMessage.body)
                                    .publishOn(Schedulers.boundedElastic())
                                    .map { it.asInputStream().readAllBytes() }
                                    .map { bytes -> String(bytes) }
                            return@body msg
                        }
                    return@exchangeToMono t.map { it to res }
                }
        return response.flatMap { rs ->
            if (!rs.second.statusCode().is2xxSuccessful) {
                logger.error("Error processing $wallet ${rs.first}")
                try {
                    val errorRs = mappingHelper.mapOne(rs.first, ErrorRs::class.java)
                    return@flatMap Mono.error(
                        object : ApiException(
                            errorRs?.errorCode ?: 5040,
                            errorRs?.message ?: "Can't create user :${rs.first}",
                        ) {},
                    )
                } catch (t: Throwable) {
                    return@flatMap Mono.error(object : ApiException(5040, "Can't process: ${rs.first}") {})
                }
            } else {
                return@flatMap Mono.empty<Void>()
            }
        }
    }
}
