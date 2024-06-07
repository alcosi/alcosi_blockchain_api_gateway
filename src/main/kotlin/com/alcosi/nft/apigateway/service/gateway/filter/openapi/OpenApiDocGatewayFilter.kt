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

/**
 * Constant variable representing the order of an open API order.
 *
 * The value of OPEN_API_ORDER is 50.
 */
const val OPEN_API_ORDER = 50

/**
 * The `OpenApiDocGatewayFilter` class is responsible for filtering requests based on file paths in the URL and returning the corresponding OpenAPI documentation file. It extends
 *  the `FileGatewayFilter` class and implements the `Logging` and `Ordered` interfaces.
 *
 * @property resourceLoader The resource loader used to load the OpenAPI documentation file.
 * @property filePath The base path of the OpenAPI documentation file.
 * @property gatewayFilterResponseWriter The response writer used to send the HTTP response.
 * @property openApiUri The URI of the OpenAPI documentation endpoint.
 * @property openDocFileRegex The regular expression used to validate the OpenAPI documentation file name.
 * @property order The order of the gateway filter in the filter chain.
 *
 * @see FileGatewayFilter
 * @see Logging
 * @see Ordered
 */
open class OpenApiDocGatewayFilter(
    resourceLoader: ResourceLoader,
    filePath: String,
    val gatewayFilterResponseWriter: GatewayFilterResponseWriter,
    openApiUri: String,
    val openDocFileRegex: Regex = "^([a-zA-Z0-9_\\-()])+(\\.yaml|\\.json)\$".toRegex(),
    private val order: Int = OPEN_API_ORDER,
) : FileGatewayFilter(filePath, gatewayFilterResponseWriter, openApiUri), Logging, Ordered {
    /**
     * The `patternResolver` variable is an instance of the `PathMatchingResourcePatternResolver` class.
     * It is used for resolving resource patterns and loading resources from a given `resourceLoader`.
     *
     * @property resourceLoader The resource loader used for loading resources.
     */
    val patternResolver = PathMatchingResourcePatternResolver(resourceLoader)

    /**
     * Represents the YAML media type.
     */
    val yamlMediaType = MediaType.parseMediaType("text/yaml")

    /**
     * Returns the order of the current filter in the filter chain.
     *
     * @return The order of the filter.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * Reads the contents of a file and returns it as a byte array.
     *
     * @param fileName The name of the file to read.
     * @return The contents of the file as a byte array.
     * @throws ApiException if the filename is invalid.
     */
    protected open fun readFile(fileName: String): ByteArray {
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

    /**
     * Filters the incoming request based on the provided URI.
     *
     * @param exchange The ServerWebExchange object representing the incoming request.
     * @param chain The GatewayFilterChain object for invoking the next filter in the chain.
     * @return A Mono that completes when the filtering is done.
     */
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
