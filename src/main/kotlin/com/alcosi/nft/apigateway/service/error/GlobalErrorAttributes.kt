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

package com.alcosi.nft.apigateway.service.error

import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import io.jsonwebtoken.ExpiredJwtException
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * GlobalErrorAttributes is an open class that extends
 * DefaultErrorAttributes and provides custom error attribute handling. It
 * overrides the getErrorAttributes function to customize the attributes
 * returned in case of an error.
 *
 * @see DefaultErrorAttributes
 */
open class GlobalErrorAttributes : DefaultErrorAttributes(), Logging {
    /**
     * Retrieves the error attributes for the given request and options.
     *
     * @param request The ServerRequest object representing the incoming
     *     request.
     * @param options The ErrorAttributeOptions that determine how the error
     *     attributes should be computed.
     * @return A Map containing the error attributes for the given request.
     */
    override fun getErrorAttributes(
        request: ServerRequest,
        options: ErrorAttributeOptions,
    ): Map<String, Any> {
        val t = super.getError(request)
        logger.error("Unhandled error ${request.exchange().request.id}", t)
        val map = super.getErrorAttributes(request, options)
        when (t) {
            is ApiException -> {
                map["status"] = t.httpCode
                map["errorCode"] = t.code
            }

            is ExpiredJwtException -> {
                map["status"] = 401
                map["errorCode"] = 4011
            }

            else -> {
                map["errorCode"] = 5000

            }
        }
        map["message"] = t.message
        map["errorClass"] = t.javaClass.name
        return map
    }
}
