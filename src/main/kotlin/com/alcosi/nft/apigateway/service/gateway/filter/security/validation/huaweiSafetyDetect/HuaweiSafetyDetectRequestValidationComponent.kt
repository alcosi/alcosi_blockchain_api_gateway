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

import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.RequestValidationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The HuaweiSafetyDetectRequestValidationComponent is responsible for validating the safety detect request
 * for Huawei devices. It extends the RequestValidationComponent class and overrides its methods to provide
 * specific validation logic. It uses various components and helper classes to perform the validation.
 *
 * @property alwaysPassed A boolean value indicating if the validation should always pass
 * @property superTokenEnabled A boolean value indicating if the super token is enabled for validation
 * @property superUserToken The super user token for validation
 * @property ttl The time-to-live in seconds for the token
 * @property packageName The expected package name for the request
 * @property webClient The WebClient instance for making HTTP requests
 * @property mappingHelper The MappingHelper instance for mapping objects
 * @property verifyUtil The HuaweiSafetyVerifySignatureComponent instance for signature verification
 * @property uniqueTokenChecker The ValidationUniqueTokenChecker instance for checking token uniqueness
 */
open class HuaweiSafetyDetectRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    protected val ttl: Long,
    protected val packageName: String,
    protected val webClient: WebClient,
    protected val mappingHelper: ObjectMapper,
    protected val verifyUtil: HuaweiSafetyVerifySignatureComponent,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : RequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, ttl, uniqueTokenChecker) {
    /**
     * The `expDelta` property represents the time delta to be added to the expiration time of a token.
     *
     * @property expDelta The time delta in seconds.
     */
    protected open val expDelta: Int = 100

    /**
     * Checks the internal validation of a token.
     *
     * @param token The token to be checked.
     * @param ip The IP address.
     * @return A Mono that emits a ValidationResult.
     */
    override fun checkInternal(
        token: String,
        ip: String?,
    ): Mono<ValidationResult> {
        return uniqueTokenChecker.isNotUnique(token, ttl + expDelta)
            .map {
                if (it) {
                    checkResponse(HuaweiSafetyDetectJwsHMSDTO(token, mappingHelper))
                } else {
                    notUniqueTokenResult
                }
            }
    }

    /**
     * Checks the response received from Huawei Safety Detect service.
     *
     * @param jwsHM The JWS HMS DTO representing the response.
     * @return The validation result indicating the success or failure of the response.
     */
    protected open fun checkResponse(jwsHM: HuaweiSafetyDetectJwsHMSDTO): ValidationResult {
        val expDate =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(jwsHM.payload.timestampMs ?: 0), ZoneId.systemDefault())
                .plusSeconds(ttl)
        return if (!packageName.equals(jwsHM.payload.apkPackageName, ignoreCase = true)) {
            return ValidationResult(false, BigDecimal.ZERO, "Package name ${jwsHM.payload.apkPackageName} is wrong")
        } else if (LocalDateTime.now().isAfter(expDate)) {
            return ValidationResult(false, BigDecimal.ZERO, "Token os too old. Expired:$expDate")
        } else if (verifyUtil.verifySignature(jwsHM)) {
            ValidationResult(true, BigDecimal.ONE)
        } else {
            ValidationResult(false, BigDecimal.ZERO, "Bad certificate")
        }
    }
}
