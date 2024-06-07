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
package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.huaweiSafetyDetect

import org.apache.logging.log4j.kotlin.Logging
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * This class implements the logic for verifying the signature of a JWS (JSON Web Signature)
 * using the Huawei Safety Detect API.
 *
 * @param certString The certificate string used for signature verification.
 * @param certType The type of the certificate. Default is "X.509".
 */
open class HuaweiSafetyVerifySignatureComponent(certString: String, val certType: String = "X.509") : Logging {
    /**
     * Represents the CA (Certificate Authority) certificate.
     *
     * The `caCert` variable is a lazy property that represents the CA certificate used for verification
     * in the TLS (Transport Layer Security) communication.
     *
     *
     * @see Certificate
     * @see readCert
     * @see certString
     * @see certType
     */
    protected open val caCert: Certificate by lazy { readCert(certString, certType) }

    /**
     * Reads a certificate from a string representation.
     *
     * @param certString The string representation of the certificate.
     * @param certType The type of the certificate (e.g., "X.509").
     * @return The parsed certificate object.
     */
    protected open fun readCert(
        certString: String,
        certType: String,
    ): Certificate {
        return certString.byteInputStream(Charset.defaultCharset())
            .use {
                val certificate = CertificateFactory.getInstance(certType).generateCertificate(it)
                return@use certificate
            }
    }

    /**
     * Verifies the signature of a HuaweiSafetyDetectJwsHMSDTO object.
     *
     * @param jws The HuaweiSafetyDetectJwsHMSDTO object to verify the signature.
     * @return true if the signature is valid, false otherwise.
     */
    open fun verifySignature(jws: HuaweiSafetyDetectJwsHMSDTO): Boolean {
        val algorithm = jws.header.alg
        if ("RS256" == algorithm) {
            val signatureAlg = Signature.getInstance("SHA256withRSA")
            return verify(signatureAlg, jws)
        } else {
            logger.error("Error checking signature, algorithm $algorithm is not supported")
            return false
        }
    }

    /**
     * Verifies the signature of a HuaweiSafetyDetectJwsHMSDTO using the given Signature algorithm.
     *
     * @param signatureAlgorithm The Signature algorithm to use for verification.
     * @param jws The HuaweiSafetyDetectJwsHMSDTO object containing the signature and x5c header.
     * @return true if the signature is valid, false otherwise.
     */
    protected open fun verify(
        signatureAlgorithm: Signature,
        jws: HuaweiSafetyDetectJwsHMSDTO,
    ): Boolean {
        try {
            val certs: List<String> = jws.header.x5c ?: listOf()
            val certificates = certs.map { readCert(it, certType) }
            verifyCertChain(certificates)
            val pubKey: PublicKey = certificates[0].publicKey
            signatureAlgorithm.initVerify(pubKey)
            signatureAlgorithm.update(jws.signContent.toByteArray(StandardCharsets.UTF_8))
            return signatureAlgorithm.verify(jws.signature)
        } catch (e: Throwable) {
            logger.error("Error checking signature ")
            return false
        }
    }

    /**
     * Verifies the certificate chain provided.
     *
     * @param certs The list of certificates in the chain to be verified.
     * @throws CertificateException if any of the certificates in the chain are invalid or the chain is broken.
     */
    protected open fun verifyCertChain(certs: List<Certificate>) {
        for (i in 0 until certs.size - 1) {
            val certificate = certs[i]
            if (certificate is X509Certificate) {
                certificate.checkValidity()
            }
            val nextCert = certs[i + 1]
            certificate.verify(nextCert.publicKey)
        }
        certs.last().verify(caCert.publicKey)
    }
}
