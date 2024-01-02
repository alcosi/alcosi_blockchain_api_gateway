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

package com.alcosi.nft.apigateway.auth.service

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.service.error.ErrorRs
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.alcosi.nft.apigateway.service.gateway.filter.security.eth.EthJwtGatewayFilter

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers


@Component
@ConditionalOnSingleCandidate(LoginRequestProcess::class)
@ConditionalOnProperty(
    matchIfMissing = true,
    prefix = "gateway.defaultRequestLoginRequestProcess",
    value = ["enabled"],
    havingValue = "true"
)
open class DefaultRequestLoginRequestProcess(
    @Value("\${gateway.microservice.uri.DefaultRequestLoginRequestProcess}") val serviceUri: String,
    @Value("\${gateway.defaultRequestLoginRequestProcess.rqTypes:}") rqTypesString: String,
    @Value("\${gateway.defaultRequestLoginRequestProcess.types:}") typesString: String,
    @Value("\${gateway.defaultRequestLoginRequestProcess.method:POST}") val method: HttpMethod,
    val webClient: WebClient,
    val mappingHelper: MappingHelper,

    ) : Logging, LoginRequestProcess {
    open val clientWalletHeader: String = EthJwtGatewayFilter.CLIENT_WALLET_HEADER
    open val clientWalletsHeader: String = EthJwtGatewayFilter.CLIENT_WALLETS_HEADER
    protected val rqTypes: List<LoginRequestProcess.REQUEST_TYPE> =
        rqTypesString.split(",").map { LoginRequestProcess.REQUEST_TYPE.valueOf(it) }
    protected val types: List<LoginRequestProcess.TYPE> =
        typesString.split(",").map { LoginRequestProcess.TYPE.valueOf(it) }

    override fun rqTypes(): List<LoginRequestProcess.REQUEST_TYPE> {
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
        val response = webClient
            .method(method())
            .uri(uri)
            .header(clientWalletHeader, wallet)
            .header(clientWalletsHeader, listOf(wallet).joinToString(","))
            .exchangeToMono { res ->
                val t = res.body { inputMessage, _ ->
                    val msg = DataBufferUtils.join(inputMessage.body)
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
                    return@flatMap Mono.error(object : ApiException(
                        errorRs?.errorCode ?: 5040,
                        errorRs?.message ?: "Can't create user :${rs.first}"
                    ) {})
                } catch (t: Throwable) {
                    return@flatMap Mono.error(object : ApiException(5040, "Can't process: ${rs.first}") {})
                }
            } else {
                return@flatMap Mono.empty<Void>()
            }
        }
    }
}