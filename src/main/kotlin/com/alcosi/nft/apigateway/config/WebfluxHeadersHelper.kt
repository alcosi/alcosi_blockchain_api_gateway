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

package com.alcosi.nft.apigateway.config

import org.springframework.http.server.reactive.ServerHttpRequest

/**
 * Utility class for working with headers in a WebFlux application.
 */
object WebfluxHeadersHelper {
    /**
     * Retrieves the value of the specified header or query parameter from the request.
     *
     * @param request The [ServerHttpRequest] object representing the incoming request.
     * @param headerName The name of the header or query parameter to retrieve.
     * @return The value of the specified header or query parameter, or null if it is not present.
     */
    fun getHeaderOrQuery(
        request: ServerHttpRequest,
        headerName: String,
    ): String? {
        val tokenString = request.headers[headerName]?.firstOrNull()
        return if (tokenString.isNullOrEmpty()) {
            request.queryParams[headerName]?.firstOrNull()
        } else {
            tokenString
        }
    }
}
