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
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


public const val SWAGGER_API_ORDER = 40

open class SwaggerApiGatewayFilter(
    val gatewayFilterResponseWriter: GatewayFilterResponseWriter,
    val resourceLoader: ResourceLoader,
    swaggerUriPath: String,
    filePath: String,
    val apiFilePath: String,
    val swaggerFileRegex : Regex = "^([a-zA-Z0-9_\\-()])+(\\.png|\\.css|\\.html|\\.js)\$".toRegex()
) : FileGatewayFilter(filePath, gatewayFilterResponseWriter, swaggerUriPath),
    Logging, Ordered {
    protected val jsMediaType = MediaType.parseMediaType("application/javascript")
    protected val cssMediaType = MediaType.parseMediaType("text/css")

    override fun getOrder(): Int {
        return SWAGGER_API_ORDER
    }

    protected fun readFile(fileName: String): ByteArray {
        if (!swaggerFileRegex.matches(fileName)){
            throw ApiException(500,"Bad filename")
        }
        val getPackage = this.javaClass.getPackage().name.replace('.', '/')
        val completeFilePath = "classpath:${getPackage}/swagger/html/${fileName}"
        val resource = resourceLoader.getResource(completeFilePath).inputStream.readAllBytes()
        val isInitializer = fileName.equals("swagger-initializer.js", true);
        if (isInitializer) {
            return resource.toString(Charsets.UTF_8).replace("@apiPath@", apiFilePath).toByteArray()
        } else {
            return resource
        }
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val matcher = getMatcher(exchange.request.path.toString())
        val response = exchange.response;
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