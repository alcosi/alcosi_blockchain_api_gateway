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
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.math.BigDecimal

/**
 * This class represents a Filter Validation Service that is responsible for validating requests using a list of
 * RequestValidators. It provides a method to check the validation of a ServerWebExchange and returns a Mono object
 * containing a ValidationResult.
 *
 * @param validationServices The list of RequestValidators to use for validation.
 * @param alwaysPassed Flag indicating whether the validation should always pass.
 * @param typeHeader The header name to use for determining the type of validation to perform.
 */
open class FilterValidationService(
    protected val validationServices: List<RequestValidator>,
    protected val alwaysPassed: Boolean,
    protected val typeHeader: String,
) : Logging {
    /**
     * Checks the validation of a ServerWebExchange and returns a Mono object containing a ValidationResult.
     *
     * @param exchange The ServerWebExchange object representing the incoming HTTP request.
     * @return A Mono object containing a ValidationResult.
     */
    fun check(exchange: ServerWebExchange): Mono<ValidationResult> {
        if (alwaysPassed) {
            return Mono.just(ValidationResult(false, BigDecimal.ONE, "Always passed mode for FilterValidationService"))
        }
        val type = exchange.request.headers[typeHeader]?.firstOrNull() ?: "GoogleCaptcha"
        val validationServiceForType =
            validationServices
                .find { it.type == type }
        if (validationServiceForType == null) {
            logger.error("Wrong type for validation $type")
            exchange.attributes[RequestValidator.Attributes.VALIDATION_IS_PASSED] = false
            return Mono.just(ValidationResult(false, BigDecimal.ZERO, "Wrong type for validation $type"))
        }
        return validationServiceForType.validate(exchange)
    }
}
