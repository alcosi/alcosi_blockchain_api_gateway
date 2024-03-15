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
