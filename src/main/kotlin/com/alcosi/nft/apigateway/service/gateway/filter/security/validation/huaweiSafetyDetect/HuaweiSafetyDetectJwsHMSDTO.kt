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

import com.alcosi.lib.objectMapper.MappingHelper
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

class HuaweiSafetyDetectJwsHMSDTO(jwsStr: String, mappingHelper: MappingHelper) {
    val header: JwsHeader
    val payload: JwsPayload
    val signContent: String
    val signature: ByteArray

    init {
        val jwsSplit = jwsStr.split(".").map { Base64.getUrlDecoder().decode(it) }
        header = mappingHelper.mapOne(String(jwsSplit[0]), JwsHeader::class.java)!!
        payload = mappingHelper.mapOne(String(jwsSplit[1]), JwsPayload::class.java)!!
        val index = jwsStr.lastIndexOf(".")
        signContent = jwsStr.substring(0, index)
        signature = jwsSplit[2]
    }

    @JvmRecord
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class JwsHeader
        @JsonCreator
        constructor(val alg: String?, val x5c: List<String>?)

    @JvmRecord
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
