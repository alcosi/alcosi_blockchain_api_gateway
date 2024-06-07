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

import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.nft.apigateway.service.error.ErrorRs
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.alcosi.nft.apigateway.service.gateway.filter.security.eth.EthJwtGatewayFilter
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * The DefaultRequestLoginRequestProcess class is an implementation of the LoginRequestProcess interface.
 * It handles the request for login process by sending a request to the specified service URI with the given wallet parameter.
 *
 * @property serviceUri The URI of the service to which the request will be sent.
 * @property rqTypes The list of request types supported by this process.
 * @property types The list of types supported by this process.
 * @property method The HTTP method to be used for the request.
 * @property webClient The WebClient instance to send the request.
 * @property mappingHelper The MappingHelper instance to map the response.
 */
open class DefaultRequestLoginRequestProcess(
    val serviceUri: String,
    val rqTypes: List<LoginRequestProcess.RequestType>,
    val types: List<LoginRequestProcess.TYPE>,
    val method: HttpMethod,
    val webClient: WebClient,
    val mappingHelper: ObjectMapper,
) : Logging, LoginRequestProcess {
    /**
     * The clientWalletHeader property represents the header name used for client wallet information in the API requests.
     *
     * @property clientWalletHeader The header name for client wallet information.
     */
    open val clientWalletHeader: String = EthJwtGatewayFilter.CLIENT_WALLET_HEADER

    /**
     * Represents the header name for client wallets.
     */
    open val clientWalletsHeader: String = EthJwtGatewayFilter.CLIENT_WALLETS_HEADER

    /**
     * Returns the list of request types supported by the login request process.
     *
     * @return the list of request types
     */
    override fun rqTypes(): List<LoginRequestProcess.RequestType> {
        return rqTypes
    }

    /**
     * Returns the list of types supported by the login request process.
     *
     * @return the list of types
     */
    override fun types(): List<LoginRequestProcess.TYPE> {
        return types
    }

    /**
     * Returns the HTTP method used by this method.
     *
     * @return the HTTP method
     */
    override fun method(): HttpMethod {
        return method
    }

    /**
     * Process the given wallet.
     *
     * @param wallet the wallet to process
     * @return a Mono that represents the completion of the processing operation
     */
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
                                    .publishOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
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
