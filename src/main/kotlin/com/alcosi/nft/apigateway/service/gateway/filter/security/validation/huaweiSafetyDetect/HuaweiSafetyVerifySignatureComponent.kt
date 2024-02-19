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
