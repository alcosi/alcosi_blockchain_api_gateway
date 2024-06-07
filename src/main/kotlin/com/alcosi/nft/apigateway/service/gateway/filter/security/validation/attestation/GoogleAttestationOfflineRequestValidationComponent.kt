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

import com.alcosi.nft.apigateway.service.error.exceptions.ApiValidationException
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.webtoken.JsonWebSignature
import com.google.api.client.json.webtoken.JsonWebToken
import com.google.api.client.util.Key
import org.apache.commons.codec.binary.Base64
import org.apache.http.conn.ssl.DefaultHostnameVerifier
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

/**
 * GoogleAttestationOfflineRequestValidationComponent is a class for validating offline Google attestation requests.
 *
 * @param alwaysPassed Indicates whether all requests should be considered passed.
 * @param superTokenEnabled Indicates whether the super user token is enabled.
 * @param superUserToken The super user token.
 * @param key The key used for validation.
 * @param packageName The package name of the application.
 * @param ttl The time-to-live value for the request.
 * @param hostname The hostname for validation.
 * @param mappingHelper The mapping helper for validation.
 * @param uniqueTokenChecker The unique token checker for validation.
 */
open class GoogleAttestationOfflineRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    key: String,
    packageName: String,
    ttl: Long,
    val hostname: String,
    mappingHelper: ObjectMapper,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : GoogleAttestationRequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, key, packageName, ttl, mappingHelper, uniqueTokenChecker) {
    /**
     * The `hostnameVerifier` variable is a protected open property of type `DefaultHostnameVerifier`.
     * It is initialized with a new instance of `DefaultHostnameVerifier`.
     * This property is used for verifying the hostname of a server during SSL/TLS certificate verification.
     * It is used in the `checkInternal` function of the `GoogleAttestationOfflineRequestValidationComponent` class.
     * The `checkInternal` function checks the validity of a token and performs attestation verification using the Google Attestation API.
     * If the token and attestation are valid, the function returns a `ValidationResult` indicating success; otherwise, it returns a `ValidationResult` indicating failure.
     */
    protected open val hostnameVerifier: DefaultHostnameVerifier = DefaultHostnameVerifier()

    /**
     * This property represents a JsonWebSignature parser used for parsing JSON Web Signatures.
     * It is initialized with a default instance of GsonFactory and the payload class AttestationStatement.
     *
     * @property parser The JsonWebSignature parser.
     */
    protected open val parser: JsonWebSignature.Parser = JsonWebSignature.parser(GsonFactory.getDefaultInstance()).setPayloadClass(AttestationStatement::class.java)

    /**
     * Represents an attestation statement containing information about a digital signature's characteristics and properties.
     *
     * @property nonce The nonce value used during the attestation process. May be null.
     * @property timestampMs The timestamp in milliseconds when the attestation statement was created. May be null.
     * @property apkPackageName The package name of the APK that was attested. May be null.
     * @property apkCertificateDigestSha256 The SHA-256 certificate digest of the APK. May be null.
     * @property apkDigestSha256 The SHA-256 hash of the APK. May be null.
     * @property ctsProfileMatch Whether the device passes the Compatibility Test Suite (CTS) profile match. May be null.
     * @property basicIntegrity Whether the device has basic integrity. May be null.
     * @property evaluationType The evaluation type of the attestation statement. May be null.
     */
    data class AttestationStatement(
        @Key
        private val nonce: String?,
        @Key
        val timestampMs: Long?,
        @Key
        val apkPackageName: String?,
        @Key
        private val apkCertificateDigestSha256: Array<String>?,
        @Key
        private val apkDigestSha256: String?,
        @Key
        val ctsProfileMatch: Boolean?,
        @Key
        val basicIntegrity: Boolean?,
        @Key
        val evaluationType: String?,
    ) : JsonWebToken.Payload() {
        /**
         * Retrieves the nonce as a byte array.
         *
         * @return The nonce as a byte array.
         */
        fun getNonce(): ByteArray {
            return Base64.decodeBase64(nonce)
        }
        /**
         * Retrieves the APK digest in SHA-256 format.
         *
         * @return the APK digest in SHA-256 format as a byte array
         */
        fun getApkDigestSha256(): ByteArray {
            return Base64.decodeBase64(apkDigestSha256)
        }
        /**
         * Retrieves the SHA-256 certificate digests of the APK.
         *
         * @return A list of byte arrays representing the SHA-256 certificate digests of the APK.
         *         If the certificate digests are not available, returns an empty list.
         */
        fun getApkCertificateDigestSha256(): List<ByteArray> {
            return apkCertificateDigestSha256?.map { Base64.decodeBase64(it) } ?: listOf()
        }
        /**
         * Checks if the evaluation type contains "BASIC".
         *
         * @return true if the evaluation type contains "BASIC", false otherwise.
         */
        fun hasBasicEvaluationType(): Boolean {
            return evaluationType?.contains("BASIC") ?: false
        }
        /**
         * Checks if the evaluation type contains "HARDWARE_BACKED".
         *
         * @return true if the evaluation type contains "HARDWARE_BACKED", false otherwise.
         */
        fun hasHardwareBackedEvaluationType(): Boolean {
            return evaluationType?.contains("HARDWARE_BACKED") ?: false
        }
    }

    /**
     * Performs internal validation using the given token value and IP address.
     *
     * @param tokenVal The token value to validate.
     * @param ip The IP address from which the request is made. Can be null.
     * @return A Mono emitting the ValidationResult of the validation process.
     */
    override fun checkInternal(
        tokenVal: String,
        ip: String?,
    ): Mono<ValidationResult> {
        return Mono.just(tokenVal)
            .subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
            .map { token ->
                try {
                    val signature: JsonWebSignature = parser.parse(token)
                    val cert = signature.verifySignature()
                    hostnameVerifier.verify(hostname, cert)
                    val stmt = signature.payload as AttestationStatement
                    checkRequest(stmt)
                    return@map okResult
                } catch (t: ApiValidationException) {
                    logger.error("GoogleAttestation error! ", t)
                    return@map ValidationResult(false, BigDecimal.ZERO, t.message)
                } catch (t: Throwable) {
                    logger.error("GoogleAttestation unknown error! ", t)
                    return@map ValidationResult(false, BigDecimal.ZERO, "Unknown error ${t.javaClass}:${t.message}")
                }
            }
    }

    /**
     * Performs request validation.
     *
     * @param rq The attestation statement representing the request.
     * @throws AttestationValidationException if any of the validation checks fail.
     */
    protected open fun checkRequest(rq: AttestationStatement) {
        if (!packageName.equals(rq.apkPackageName, ignoreCase = true)) {
            throw AttestationValidationException("Invalid apkPackageName")
        }
        if (Instant.now().epochSecond > ((rq.timestampMs ?: 0) + ttl)) {
            throw AttestationValidationException("Request too old. TTL $ttl")
        }
        if (rq.basicIntegrity != true) {
            throw AttestationValidationException("Basic integrity is failed")
        }
    }
}
