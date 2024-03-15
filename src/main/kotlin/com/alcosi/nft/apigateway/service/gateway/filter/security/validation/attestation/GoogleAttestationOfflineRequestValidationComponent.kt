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
import com.alcosi.nft.apigateway.service.error.exceptions.ApiValidationException
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.json.webtoken.JsonWebSignature
import com.google.api.client.json.webtoken.JsonWebToken
import com.google.api.client.util.Key
import org.apache.commons.codec.binary.Base64
import org.apache.http.conn.ssl.DefaultHostnameVerifier
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigDecimal
import java.time.Instant

open class GoogleAttestationOfflineRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    key: String,
    packageName: String,
    ttl: Long,
    val hostname: String,
    mappingHelper: MappingHelper,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : GoogleAttestationRequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, key, packageName, ttl, mappingHelper, uniqueTokenChecker) {
    protected open val hostnameVerifier: DefaultHostnameVerifier = DefaultHostnameVerifier()
    protected open val parser: JsonWebSignature.Parser = JsonWebSignature.parser(GsonFactory.getDefaultInstance()).setPayloadClass(AttestationStatement::class.java)

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
        fun getNonce(): ByteArray {
            return Base64.decodeBase64(nonce)
        }

        fun getApkDigestSha256(): ByteArray {
            return Base64.decodeBase64(apkDigestSha256)
        }

        fun getApkCertificateDigestSha256(): List<ByteArray> {
            return apkCertificateDigestSha256?.map { Base64.decodeBase64(it) } ?: listOf()
        }

        fun hasBasicEvaluationType(): Boolean {
            return evaluationType?.contains("BASIC") ?: false
        }

        fun hasHardwareBackedEvaluationType(): Boolean {
            return evaluationType?.contains("HARDWARE_BACKED") ?: false
        }
    }

    override fun checkInternal(
        tokenVal: String,
        ip: String?,
    ): Mono<ValidationResult> {
        return Mono.just(tokenVal)
            .subscribeOn(Schedulers.boundedElastic())
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
