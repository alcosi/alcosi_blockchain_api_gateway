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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.attestation

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.codec.binary.Base64
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

open class GoogleAttestationOnlineRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    key: String,
    packageName: String,
    ttl: Long,
    val uri: String,
    val webClient: WebClient,
    mappingHelper: MappingHelper,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : GoogleAttestationRequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, key, packageName, ttl, mappingHelper, uniqueTokenChecker) {
    @JvmRecord
    data class VerificationRequest
        @JsonCreator
        constructor(val signedAttestation: String?)

    @JvmRecord
    data class VerificationResponse
        @JsonCreator
        constructor(
            @JsonProperty("isValidSignature") val isValidSignature: Boolean?,
            @JsonProperty("error") val error: String?,
        )

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JvmRecord
    data class Request
        @JsonCreator
        constructor(
            @JsonProperty("timestampMs") val timestamp: Long,
            @JsonProperty("nonce") val nonce: String,
            @JsonProperty("apkPackageName") val apkPackageName: String,
            @JsonProperty("apkDigestSha256") val apkDigestSha256: String,
            @JsonProperty("apkCertificateDigestSha256") val apkCertificateDigestSha256: Set<String>,
            @JsonProperty("ctsProfileMatch") val ctsProfileMatch: Boolean,
            @JsonProperty("basicIntegrity") val basicIntegrity: Boolean,
        )

    protected open fun mapRequest(body: String): Request {
        try {
            logger.debug("Attestation request : $body")
            return mappingHelper.mapOne(body, Request::class.java)!!
        } catch (t: Throwable) {
            logger.error("mapRequest error", t)
            throw AttestationValidationException("Invalid ")
        }
    }

    protected open fun getJwtBody(token: String): String {
        try {
            val payload = extractJwsData(token)!!
            logger.debug("JWT $payload")
            return payload
        } catch (t: Throwable) {
            logger.error("extractJwsData error", t)
            throw AttestationValidationException("Invalid token body")
        }
    }

    protected open fun extractJwsData(jws: String): String {
        val parts = jws.split(".")
        return if (parts.size != 3) {
            val msg = "Failure: Illegal JWS signature format. The JWS consists of ${parts.size} parts instead of 3."
            logger.error(msg)
            throw AttestationValidationException(msg)
        } else {
            String(Base64.decodeBase64(parts[1]))
        }
    }

    override fun checkInternal(
        token: String,
        ip: String?,
    ): Mono<ValidationResult> {
        checkRequest(mapRequest(getJwtBody(token)))
        val googleRs =
            webClient.post().uri { builder -> builder.path(uri).queryParam("key", key).build() }
                .bodyValue(VerificationRequest(token))
                .exchangeToMono { res ->
                    if (res.statusCode().is2xxSuccessful) {
                        res.bodyToMono<String>().map { bodyRaw ->
                            val body = mappingHelper.mapOne(bodyRaw, VerificationResponse::class.java)
                            if (body?.isValidSignature == true) {
                                okResult
                            } else {
                                ValidationResult(false, BigDecimal.ZERO, "Not valid signature:${body?.error}")
                            }
                        }
                    } else {
                        Mono.just(
                            ValidationResult(
                                false,
                                BigDecimal.ZERO,
                                "Not valid attestation response! ${res.statusCode().value()}",
                            ),
                        )
                    }
                }.onErrorResume { t ->
                    logger.info("Error!", t)
                    Mono.just(ValidationResult(false, BigDecimal.ZERO, "${t.javaClass.simpleName}:${t.message}"))
                }

        return googleRs
    }

    protected open fun checkRequest(rq: Request) {
        if (!packageName.equals(rq.apkPackageName, ignoreCase = true)) {
            throw AttestationValidationException("Invalid apkPackageName")
        }
        if (Instant.now().epochSecond > (rq.timestamp + ttl)) {
            throw AttestationValidationException("Request too old. TTL $ttl")
        }
        if (!rq.basicIntegrity) {
            throw AttestationValidationException("Basic integrity is failed")
        }
    }
}
