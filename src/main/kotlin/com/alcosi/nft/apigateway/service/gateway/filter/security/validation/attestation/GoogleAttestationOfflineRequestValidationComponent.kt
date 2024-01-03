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

import com.alcosi.lib.object_mapper.MappingHelper
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
    ) : GoogleAttestationRequestValidationComponent(alwaysPassed,superTokenEnabled,superUserToken, key, packageName, ttl,mappingHelper,uniqueTokenChecker) {
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
            return Base64.decodeBase64(nonce);
        }

        fun getApkDigestSha256(): ByteArray {
            return Base64.decodeBase64(apkDigestSha256);
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


    override fun checkInternal(tokenVal: String, ip: String?): Mono<ValidationResult> {
        return Mono.just(tokenVal)
            .subscribeOn(Schedulers.boundedElastic())
            .map {token->
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
            throw AttestationValidationException("Request too old. TTL ${ttl}")
        }
        if (rq.basicIntegrity != true) {
            throw AttestationValidationException("Basic integrity is failed")
        }
    }
}