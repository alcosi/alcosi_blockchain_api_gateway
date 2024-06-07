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

import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.codec.binary.Base64
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

/**
 * This class represents a component for performing online request validation using Google Attestation.
 *
 * @property alwaysPassed Whether the validation should always pass.
 * @property superTokenEnabled Whether the super token is enabled for validation.
 * @property superUserToken The super user token used for validation.
 * @property key The API key to access the Google Attestation service.
 * @property packageName The package name of the application.
 * @property ttl The TTL (time-to-live) in milliseconds for the request.
 * @property uri The URI of the Google Attestation service.
 * @property webClient The WebClient used for making HTTP requests.
 * @property mappingHelper The helper class for mapping JSON to objects.
 * @property uniqueTokenChecker The token checker for checking the uniqueness of the token.
 */
open class GoogleAttestationOnlineRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    key: String,
    packageName: String,
    ttl: Long,
    val uri: String,
    val webClient: WebClient,
    mappingHelper: ObjectMapper,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : GoogleAttestationRequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, key, packageName, ttl, mappingHelper, uniqueTokenChecker) {
    /**
     * Represents a verification request for Google attestation.
     *
     * @property signedAttestation The signed attestation string.
     * @constructor Creates a VerificationRequest.
     */
    data class VerificationRequest
        @JsonCreator
        constructor(val signedAttestation: String?)

    /**
     * Represents the response of the verification process.
     *
     * @property isValidSignature Indicates whether the signature is valid.
     * @property error The error message, if any.
     */
    data class VerificationResponse
        @JsonCreator
        constructor(
            @JsonProperty("isValidSignature") val isValidSignature: Boolean?,
            @JsonProperty("error") val error: String?,
        )

    /**
     * Represents a request object used for certain operations.
     *
     * This class is a data class annotated with `JsonIgnoreProperties(ignoreUnknown = true)` to ignore any unknown properties
     * when deserializing the JSON into an object.
     *
     * @property timestamp The timestamp of the request in milliseconds.
     * @property nonce The unique identifier (nonce) of the request.
     * @property apkPackageName The package name of the APK.
     * @property apkDigestSha256 The SHA-256 digest hash of the APK.
     * @property apkCertificateDigestSha256 A set of SHA-256 digest hashes of the APK's certificates.
     * @property ctsProfileMatch Indicates if the device passes the Compatibility Test Suite (CTS) integrity check.
     * @property basicIntegrity Indicates if the device has basic integrity.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
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

    /**
     * Maps the given JSON body to a Request object.
     *
     * @param body The JSON body string to be mapped.
     * @return The mapped Request object.
     * @throws AttestationValidationException If there is an error during mapping.
     */
    protected open fun mapRequest(body: String): Request {
        try {
            logger.debug("Attestation request : $body")
            return mappingHelper.mapOne(body, Request::class.java)!!
        } catch (t: Throwable) {
            logger.error("mapRequest error", t)
            throw AttestationValidationException("Invalid ")
        }
    }

    /**
     * Retrieves the body of the JWT token.
     *
     * @param token The JWT token.
     * @return The body of the JWT token.
     */
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

    /**
     * Extracts the JWS data from the given JWS signature.
     *
     * @param jws The JWS signature.
     * @return The extracted JWS data.
     * @throws AttestationValidationException If there is an error during extraction.
     */
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

    /**
     * Performs internal validation of a token using Google attestation.
     *
     * @param token The token to be validated.
     * @param ip The IP address associated with the token (optional).
     * @return A Mono emitting a ValidationResult object indicating the result of the validation.
     */
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

    /**
     * Checks the validity of a request.
     *
     * @param rq The request object to be checked.
     * @throws AttestationValidationException If the request is invalid.
     */
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
