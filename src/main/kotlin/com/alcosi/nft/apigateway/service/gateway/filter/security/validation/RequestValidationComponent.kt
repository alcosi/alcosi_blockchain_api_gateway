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

abstract class RequestValidationComponent(
    val alwaysPassed: Boolean,
    val superTokenEnabled: Boolean,
    val superUserToken: String,
    val uniqueTokenTTL: Long,
    val uniqueTokenChecker: ValidationUniqueTokenChecker,
) : Logging {
    protected open val noTokenResult = ValidationResult(false, BigDecimal.ZERO, "No token")
    protected open val notUniqueTokenResult = ValidationResult(false, BigDecimal.ZERO, "Not unique token")
    protected open val alwaysPassedResult = ValidationResult(true, BigDecimal.ONE, "Always passed mode")
    protected open val superTokenResult = ValidationResult(true, BigDecimal.ONE, "Super token mode")
    protected open val okResult = ValidationResult(true, BigDecimal.ONE)

    abstract fun checkInternal(
        token: String,
        ip: String?,
    ): Mono<ValidationResult>

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
