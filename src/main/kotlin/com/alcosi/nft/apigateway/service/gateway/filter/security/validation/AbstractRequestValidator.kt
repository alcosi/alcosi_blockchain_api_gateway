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

import org.apache.logging.log4j.kotlin.logger
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * This abstract class provides a base implementation of a request validator.
 *
 * @property validationComponent The request validation component to use for validation.
 * @property type The type of the validator.
 * @property tokenHeader The header name for the token.
 * @property ipHeader The header name for the IP address.
 */
abstract class AbstractRequestValidator(protected open val validationComponent: RequestValidationComponent, override val type: String, protected val tokenHeader: String, protected val ipHeader: String) : RequestValidator {
    /**
     * Validates the request of the given [exchange] using the provided token and IP address headers.
     * Returns a [Mono] that emits a [ValidationResult].
     *
     * @param exchange The server web exchange containing the request to validate.
     * @return A [Mono] emitting a [ValidationResult].
     */
    override fun validate(exchange: ServerWebExchange): Mono<ValidationResult> {
        val token = exchange.request.headers[tokenHeader]?.firstOrNull()
        val ip = exchange.request.headers[ipHeader]?.firstOrNull()
        val validationResult = validationComponent.check(token, ip)
        return validationResult.map {
            if (!it.success) {
                logger.info("ValidationService success:false.${it.errorDescription}")
            }
            it
        }
    }
}
