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

package com.alcosi.nft.apigateway.service.gateway.filter.openapi

import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.Ordered
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.io.File
import java.time.Duration

const val OPEN_API_ORDER = 50

open class OpenApiDocGatewayFilter(
    resourceLoader: ResourceLoader,
    filePath: String,
    val gatewayFilterResponseWriter: GatewayFilterResponseWriter,
    openApiUri: String,
    val openDocFileRegex: Regex = "^([a-zA-Z0-9_\\-()])+(\\.yaml|\\.json)\$".toRegex(),
    private val order: Int = OPEN_API_ORDER,
) : FileGatewayFilter(filePath, gatewayFilterResponseWriter, openApiUri),
    Logging,
    Ordered {
    val patternResolver = PathMatchingResourcePatternResolver(resourceLoader)
    val yamlMediaType = MediaType.parseMediaType("text/yaml")

    override fun getOrder(): Int {
        return order
    }

    protected fun readFile(fileName: String): ByteArray {
        if (!openDocFileRegex.matches(fileName)) {
            throw ApiException(500, "Bad filename")
        }
        val inputStream =
            if (!filePath.startsWith("classpath", true)) {
                val path = filePath + fileName
                File(path).inputStream()
            } else {
                patternResolver.getResource("$filePath$fileName").inputStream
            }
        return inputStream.readAllBytes()
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val uri = exchange.request.path.toString()
        val matcher = getMatcher(uri)
        val response = exchange.response
        val matches = matcher.matches()
        if (matches) {
            val filePath = matcher.group(2)
            val file = readFile(filePath)
            if (file == null) {
                return write404(response)
            } else {
                val cachceControl =
                    CacheControl
                        .maxAge(Duration.ofDays(1))
                        .cachePublic()
                response.headers.cacheControl = cachceControl.headerValue
                if (filePath.endsWith("yaml", true) || filePath.endsWith("yml", true)) {
                    response.headers.contentType = yamlMediaType
                } else if (filePath.endsWith("json", true)) {
                    response.headers.contentType = MediaType.APPLICATION_JSON
                } else {
                    response.headers.contentType = MediaType.APPLICATION_OCTET_STREAM
                }
                return gatewayFilterResponseWriter.writeByteArray(response, file)
            }
        } else {
            return write404(response)
        }
    }
}
