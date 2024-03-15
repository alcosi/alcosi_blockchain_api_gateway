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

open class HuaweiSafetyVerifySignatureComponent(certString: String, val certType: String = "X.509") : Logging {
    protected open val caCert: Certificate by lazy { readCert(certString, certType) }

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
