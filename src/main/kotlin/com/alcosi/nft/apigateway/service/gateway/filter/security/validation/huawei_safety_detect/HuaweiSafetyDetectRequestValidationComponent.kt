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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.huawei_safety_detect

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.RequestValidationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

open class HuaweiSafetyDetectRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    protected val ttl: Long,
    protected val packageName: String,
    protected val webClient: WebClient,
    protected val mappingHelper: MappingHelper,
    protected val verifyUtil: HuaweiSafetyVerifySignatureComponent,
     uniqueTokenChecker: ValidationUniqueTokenChecker
) : RequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken,ttl,uniqueTokenChecker) {
    protected open val expDelta: Int = 100

    override fun checkInternal(token: String, ip: String?): Mono<ValidationResult> {
        return uniqueTokenChecker.isNotUnique(token, ttl + expDelta)
            .map {
                if (it) {
                    checkResponse(HuaweiSafetyDetectJwsHMSDTO(token, mappingHelper))
                } else {
                    notUniqueTokenResult
                }
            }
    }

    protected open fun checkResponse(jwsHM: HuaweiSafetyDetectJwsHMSDTO): ValidationResult {
        val expDate =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(jwsHM.payload.timestampMs ?: 0), ZoneId.systemDefault())
                .plusSeconds(ttl)
        return if (!packageName.equals(jwsHM.payload.apkPackageName, ignoreCase = true)) {
            return ValidationResult(false, BigDecimal.ZERO, "Package name ${jwsHM.payload.apkPackageName} is wrong")
        } else if (LocalDateTime.now().isAfter(expDate)) {
            return ValidationResult(false, BigDecimal.ZERO, "Token os too old. Expired:${expDate}")
        } else if (verifyUtil.verifySignature(jwsHM)) {
            ValidationResult(true, BigDecimal.ONE)
        } else {
            ValidationResult(false, BigDecimal.ZERO, "Bad certificate")
        }
    }

}