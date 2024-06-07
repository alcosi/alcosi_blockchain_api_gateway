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
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Swagger API Order.
 *
 * Represents the order value for Swagger API in the GatewayFilter chain.
 * The Swagger API order is set to 40.
 */
public const val SWAGGER_API_ORDER = 40

/**
 * SwaggerApiGatewayFilter is an open class that extends FileGatewayFilter.
 * It is responsible for filtering requests based on file paths in the URL and handling Swagger API requests.
 *
 * @property gatewayFilterResponseWriter The GatewayFilterResponseWriter used for writing HTTP responses.
 * @property resourceLoader The ResourceLoader used for loading Swagger API resources.
 * @property swaggerUriPath The URI path for Swagger API requests.
 * @property filePath The base path of the Swagger API files.
 * @property apiFilePath The API file path used for replacing placeholders in the Swagger initializer file.
 * @property swaggerFileRegex The regex pattern for matching Swagger API files.
 * @property order The order value for ordering of filters.
 */
open class SwaggerApiGatewayFilter(
    val gatewayFilterResponseWriter: GatewayFilterResponseWriter,
    val resourceLoader: ResourceLoader,
    swaggerUriPath: String,
    filePath: String,
    val apiFilePath: String,
    val swaggerFileRegex: Regex = "^([a-zA-Z0-9_\\-()])+(\\.png|\\.css|\\.html|\\.js)\$".toRegex(),
    private val order: Int = SWAGGER_API_ORDER,
) : FileGatewayFilter(filePath, gatewayFilterResponseWriter, swaggerUriPath), Logging, Ordered {
    /**
     * Represents the media type for JavaScript files.
     */
    protected val jsMediaType = MediaType.parseMediaType("application/javascript")

    /**
     * Represents the media type for CSS files.
     */
    protected val cssMediaType = MediaType.parseMediaType("text/css")

    /**
     * Returns the order value of the SwaggerApiGatewayFilter class.
     *
     * @return The order value.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * Reads a file as a byte array.
     *
     * @param fileName The name of the file to read.
     * @return The contents of the file as a byte array.
     * @throws ApiException if the file name doesn't match the regex pattern.
     */
    protected fun readFile(fileName: String): ByteArray {
        if (!swaggerFileRegex.matches(fileName)) {
            throw ApiException(500, "Bad filename")
        }
        val getPackage = this.javaClass.getPackage().name.replace('.', '/')
        val completeFilePath = "classpath:$getPackage/swagger/html/$fileName"
        val resource = resourceLoader.getResource(completeFilePath).inputStream.readAllBytes()
        val isInitializer = fileName.equals("swagger-initializer.js", true)
        if (isInitializer) {
            return resource.toString(Charsets.UTF_8).replace("@apiPath@", apiFilePath).toByteArray()
        } else {
            return resource
        }
    }

    /**
     * Filters the request based on the provided conditions.
     *
     * @param exchange The ServerWebExchange object representing the request and response.
     * @param chain The GatewayFilterChain object representing the filter chain.
     * @return A Mono that completes when the response has been processed.
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val matcher = getMatcher(exchange.request.path.toString())
        val response = exchange.response
        val matches = matcher.matches()
        if (matches) {
            val filePath = matcher.group(2)
            val file = readFile(filePath)
            if (file == null) {
                return write404(response)
            } else {
                response.setRawStatusCode(200)
                if (filePath.endsWith("html", true)) {
                    response.headers.contentType = MediaType.TEXT_HTML
                } else if (filePath.endsWith("png", true)) {
                    response.headers.contentType = MediaType.IMAGE_PNG
                } else if (filePath.endsWith("js", true)) {
                    response.headers.contentType = jsMediaType
                } else if (filePath.endsWith("css", true)) {
                    response.headers.contentType = cssMediaType
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
