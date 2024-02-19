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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.iosDeviceCheck

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.RequestValidationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.*

open class IOSDeviceCheckRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    ttl: Long,
    protected val appleServerUrl: String,
    protected val webClient: WebClient,
    protected val mappingHelper: MappingHelper,
    protected val jwtComponent: IOSDeviceCheckJWTComponent,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : RequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, ttl, uniqueTokenChecker) {
    @JvmRecord
    data class VerificationRequest
        @JsonCreator
        constructor(
            @JsonProperty(value = "device_token")
            val deviceToken: String,
            @JsonProperty(value = "transaction_id")
            val transactionId: UUID,
            @JsonProperty(value = "timestamp")
            val timestamp: Long,
        )

    override fun checkInternal(
        token: String,
        ip: String?,
    ): Mono<ValidationResult> {
        val request =
            webClient
                .post()
                .uri(appleServerUrl)
                .headers { createHeaders() }
                .bodyValue(VerificationRequest(token, UUID.randomUUID(), System.currentTimeMillis()))
        val appleRs =
            request
                .exchangeToMono { res ->
                    val success = res.statusCode().is2xxSuccessful
                    res.bodyToMono<String>()
                        .switchIfEmpty(Mono.just(""))
                        .map { bodyRaw ->
                            ValidationResult(
                                success,
                                if (success) BigDecimal.ONE else BigDecimal.ZERO,
                                bodyRaw,
                            )
                        }
                }.onErrorResume { t ->
                    logger.info("Error!", t)
                    Mono.just(ValidationResult(false, BigDecimal.ZERO, "${t.javaClass.simpleName}:${t.message}"))
                }
        return appleRs
    }

    protected open fun createHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.acceptCharset = listOf(StandardCharsets.UTF_8)
        headers.add("Authorization", "Bearer ${jwtComponent.getJWTString()}")
        return headers
    }
}
