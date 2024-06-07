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

import com.alcosi.lib.objectMapper.mapOne
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*

/**
 * Represents a Huawei Safety Detect Jws HMS DTO.
 *
 * @property jwsStr The JWS string.
 * @property mappingHelper The mapping helper.
 * @property header The JWS header.
 * @property payload The JWS payload.
 * @property signContent The sign content.
 * @property signature The signature.
 */
open class HuaweiSafetyDetectJwsHMSDTO(jwsStr: String, mappingHelper: ObjectMapper) {
    val header: JwsHeader
    val payload: JwsPayload
    val signContent: String
    val signature: ByteArray

    /**
     * Initializes the properties by splitting the JWS string and mapping
     * the components to the corresponding properties using the MappingHelper
     */
    init {
        val jwsSplit = jwsStr.split(".").map { Base64.getUrlDecoder().decode(it) }
        header = mappingHelper.mapOne(String(jwsSplit[0]), JwsHeader::class.java)!!
        payload = mappingHelper.mapOne(String(jwsSplit[1]), JwsPayload::class.java)!!
        val index = jwsStr.lastIndexOf(".")
        signContent = jwsStr.substring(0, index)
        signature = jwsSplit[2]
    }

    /**
     * Represents the header section of a JWS (JSON Web Signature).
     *
     * @property alg The algorithm used for signing the JWS.
     * @property x5c A list of base64-encoded X.509 certificates.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class JwsHeader
        @JsonCreator
        constructor(val alg: String?, val x5c: List<String>?)

    /**
     * Represents the payload section of a JWS (JSON Web Signature).
     *
     * @property nonce The nonce value.
     * @property apkPackageName The package name of the APK.
     * @property apkDigestSha256 The SHA-256 digest of the APK.
     * @property apkCertificateDigestSha256 The list of SHA-256 digests of the APK certificates.
     * @property isBasicIntegrity A boolean indicating the basic integrity of the APK.
     * @property timestampMs The timestamp in milliseconds.
     * @property advice The advice message.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class JwsPayload
        @JsonCreator
        constructor(
            val nonce: String?,
            val apkPackageName: String?,
            val apkDigestSha256: String?,
            val apkCertificateDigestSha256: List<String>?,
            val isBasicIntegrity: Boolean?,
            val timestampMs: Long?,
            val advice: String?,
        )
}
