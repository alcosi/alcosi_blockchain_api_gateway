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


    abstract fun checkInternal(token: String, ip: String?): Mono<ValidationResult>

    open fun check(token: String?, ip: String?): Mono<ValidationResult> {
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