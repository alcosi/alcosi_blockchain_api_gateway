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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation

import org.apache.logging.log4j.kotlin.Logging
import reactor.core.publisher.Mono
import java.math.BigDecimal

/**
 * RequestValidationComponent is an abstract class that serves as a base for request validation components.
 *
 * @property alwaysPassed A boolean indicating if the validation always passes.
 * @property superTokenEnabled A boolean indicating if the super token is enabled.
 * @property superUserToken The super user token string.
 * @property uniqueTokenTTL The time-to-live value in milliseconds for unique tokens.
 * @property uniqueTokenChecker The ValidationUniqueTokenChecker instance.
 */
abstract class RequestValidationComponent(
    val alwaysPassed: Boolean,
    val superTokenEnabled: Boolean,
    val superUserToken: String,
    val uniqueTokenTTL: Long,
    val uniqueTokenChecker: ValidationUniqueTokenChecker,
) : Logging {
    /**
     * Represents the result of a validation process.
     *
     * @property success Indicates whether the validation was successful.
     * @property score The score associated with the validation.
     * @property errorDescription The description of any error encountered during validation.
     */
    protected open val noTokenResult = ValidationResult(false, BigDecimal.ZERO, "No token")

    /**
     * Represents the result of a validation process for a non-unique token.
     *
     * @property success Indicates whether the validation was successful. Always false for not unique token.
     * @property score The score of the validation result. Always BigDecimal.ZERO for not unique token.
     * @property errorDescription The description of the error. Always "Not unique token" for not unique token.
     */
    protected open val notUniqueTokenResult = ValidationResult(false, BigDecimal.ZERO, "Not unique token")
    /**
     * Represents the result of validation that will always be considered as passed.
     *
     * @property result The validation result. Always true.
     * @property value The value associated with the result. Always BigDecimal.ONE.
     * @property message The message describing the validation result. Always "Always passed mode".
     */
    protected open val alwaysPassedResult = ValidationResult(true, BigDecimal.ONE, "Always passed mode")
    /**
     * Represents the result of a super token validation.
     *
     * @property isValid A flag indicating whether the super token is valid or not.
     * @property value The value associated with the super token.
     * @property description A description of the super token mode.
     */
    protected open val superTokenResult = ValidationResult(true, BigDecimal.ONE, "Super token mode")
    /**
     * Represents a valid result of a validation process.
     *
     * @property success Indicates if the validation was successful.
     * @property score The score of the validation.
     * @property errorDescription The description of any error that occurred during the validation.
     */
    protected open val okResult = ValidationResult(true, BigDecimal.ONE)

    /**
     * Checks the validity of a token and IP address.
     *
     * @param token The token to check for validity. Can be null or blank.
     * @param ip The IP address associated with the token.
     * @return A Mono containing the validation result.
     */
    abstract fun checkInternal(
        token: String,
        ip: String?,
    ): Mono<ValidationResult>

    /**
     * Checks the validity of a token and IP address.
     *
     * @param token The token to check for validity. Can be null or blank.
     * @param ip The IP address associated with the token.
     * @return A Mono containing the validation result.
     */
    open fun check(
        token: String?,
        ip: String?,
    ): Mono<ValidationResult> {
        if (alwaysPassed) {
            return Mono.just(alwaysPassedResult)
        }
        if (token.isNullOrBlank()) {
            return Mono.just(noTokenResult)
        }
        if (superUserToken == token) {
            if (superTokenEnabled) {
                return Mono.just(superTokenResult)
            }
        }
        return uniqueTokenChecker.isNotUnique(token, uniqueTokenTTL).flatMap {
            if (it) {
                return@flatMap checkInternal(token, ip)
            } else {
                return@flatMap Mono.just(notUniqueTokenResult)
            }
        }
    }
}
