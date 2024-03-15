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

package com.alcosi.nft.apigateway.service.gateway.filter.openapi

import com.alcosi.nft.apigateway.service.gateway.filter.ControllerGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import reactor.core.publisher.Mono
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class FileGatewayFilter(
    val filePath: String,
    val writer: GatewayFilterResponseWriter,
    urlPath: String,
) : ControllerGatewayFilter,
    Logging {
    protected val error404 =
        """
        {"errorCode":4040,"message":"no such file"}
        """.trimIndent().toByteArray()
    protected val regex: Pattern = "($urlPath)(.*)".replace("/", "\\/").toPattern()

    fun getMatcher(uri: String): Matcher {
        return regex.matcher(uri)
    }

    override fun matches(request: ServerHttpRequest): Boolean {
        return getMatcher(request.path.toString()).matches()
    }

    fun write404(response: ServerHttpResponse): Mono<Void> {
        response.setRawStatusCode(404)
        response.headers.contentType = MediaType.APPLICATION_JSON
        return writer.writeByteArray(response, error404)
    }
}
