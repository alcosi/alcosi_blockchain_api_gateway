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

import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.RequestValidationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * The `IOSDeviceCheckRequestValidationComponent` class is responsible for validating iOS device check requests.
 *
 * @param alwaysPassed Determines if requests are always passed without validation.
 * @param superTokenEnabled Determines if a super token is enabled for bypassing validation.
 * @param superUserToken The super token used for bypassing validation.
 * @param ttl The time-to-live for the validation result in milliseconds.
 * @param appleServerUrl The URL of the Apple server for device check requests.
 * @param webClient The WebClient used for making HTTP requests to the Apple server.
 * @param mappingHelper The MappingHelper used for mapping JSON responses.
 * @param jwtComponent The IOSDeviceCheckJWTComponent used for generating JWTs.
 * @param uniqueTokenChecker The ValidationUniqueTokenChecker used for checking token uniqueness.
 */
open class IOSDeviceCheckRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    ttl: Long,
    protected val appleServerUrl: String,
    protected val webClient: WebClient,
    protected val mappingHelper: ObjectMapper,
    protected val jwtComponent: IOSDeviceCheckJWTComponent,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : RequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, ttl, uniqueTokenChecker) {
    /**
     * The `VerificationRequest` class represents a request for verification.
     *
     * @property deviceToken The device token string.
     * @property transactionId The transaction ID.
     * @property timestamp The timestamp of the request.
     */
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

    /**
     * Performs internal validation using an Apple server.
     *
     * @param token The token string to be validated.
     * @param ip The IP address from where the validation request originates.
     * @return A Mono that emits a ValidationResult object.
     */
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

    /**
     * Creates the HttpHeaders object for making HTTP requests.
     *
     * @return The HttpHeaders object.
     */
    protected open fun createHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.acceptCharset = listOf(StandardCharsets.UTF_8)
        headers.add("Authorization", "Bearer ${jwtComponent.getJWTString()}")
        return headers
    }
}
