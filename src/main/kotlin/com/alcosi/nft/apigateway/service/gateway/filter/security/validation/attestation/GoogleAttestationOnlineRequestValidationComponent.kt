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
