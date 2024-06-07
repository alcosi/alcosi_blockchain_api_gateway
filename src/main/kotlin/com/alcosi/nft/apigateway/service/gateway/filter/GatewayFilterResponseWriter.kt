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

package com.alcosi.nft.apigateway.service.gateway.filter

import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.lib.objectMapper.serialize
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * GatewayFilterResponseWriter is a class responsible for writing responses in JSON format to the server's response.
 *
 * @property mappingHelper The ObjectMapper instance used for serialization and deserialization of JSON.
 */
open class GatewayFilterResponseWriter(val mappingHelper: ObjectMapper) : Logging {
    /**
     * Reads the body of the given ServerWebExchange request.
     *
     * @param exchange the ServerWebExchange object representing the current HTTP request and response
     * @return a Mono emitting the body of the request as a ByteArray
     */
    protected open fun readBody(exchange: ServerWebExchange): Mono<ByteArray> {
        val bodyAsStringMono: Mono<ByteArray> = getBodyAsBytes(exchange.request.body)
        return bodyAsStringMono
    }

    /**
     * Retrieves the body of a Flux<DataBuffer> as a byte array.
     *
     * @param data The Flux<DataBuffer> containing the body content.
     * @return A Mono<ByteArray> that represents the body of the Flux<DataBuffer>.
     */
    open fun getBodyAsBytes(data: Flux<DataBuffer>): Mono<ByteArray> {
        return DataBufferUtils.join(data)
            .publishOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
            .map {
                it.asInputStream().readAllBytes()
            }
    }

    /**
     * Reads the body of the given ServerWebExchange request.
     *
     * @param exchange the ServerWebExchange object representing the current HTTP request and response
     * @param clazz the Class representing the expected type of the body
     * @return a Mono emitting the body of the request as an instance of the specified class
     */
    @Suppress("UNCHECKED_CAST")
    open fun <T> readBody(
        exchange: ServerWebExchange,
        clazz: Class<T>,
    ): Mono<T> {
        val map = readBody(exchange).map { mappingHelper.mapOne(String(it), clazz as Class<Any>) as T }
        return map
    }

    /**
     * Writes the given data as JSON to the provided ServerHttpResponse.
     *
     * @param response The ServerHttpResponse object representing the HTTP response.
     * @param data The data to be written as JSON.
     * @return A Mono<Void> representing the asynchronous result of the method.
     */
    open fun writeJson(
        response: ServerHttpResponse,
        data: Any,
    ): Mono<Void> {
        val serialize = mappingHelper.serialize(data)
        return writeByteArrayJson(response, serialize!!.toByteArray())
    }

    /**
     * Writes the provided data as JSON to the specified ServerHttpResponse.
     *
     * @param response The ServerHttpResponse object representing the HTTP response.
     * @param data The data to be written as JSON.
     * @return A Mono<Void> representing the asynchronous result of the method.
     */
    open fun writeMonoJson(
        response: ServerHttpResponse,
        data: Mono<out Any>,
    ): Mono<Void> {
        return writeMonoByteArrayJson(response, data.map { mappingHelper.serialize(it)!!.toByteArray() })
    }

    /**
     * Writes a byte array as JSON response.
     *
     * @param response The ServerHttpResponse object to write the response to.
     * @param data The byte array to be written as the response body.
     * @return A Mono that represents the completion of the response writing process.
     */
    open fun writeByteArrayJson(
        response: ServerHttpResponse,
        data: ByteArray,
    ): Mono<Void> {
        response.setRawStatusCode(200)
        response.headers.contentType = MediaType.APPLICATION_JSON
        val dataBuffer = response.bufferFactory().wrap(data)
        val just = Flux.just(dataBuffer)
        return response.writeWith(just)
    }

    /**
     * Writes the specified Mono of ByteArray as JSON to the ServerHttpResponse.
     *
     * @param response The server response to write to.
     * @param data The Mono of ByteArray containing the JSON data to be written.
     * @return A Mono representing the completion of writing the JSON data to the response.
     */
    protected open fun writeMonoByteArrayJson(
        response: ServerHttpResponse,
        data: Mono<ByteArray>,
    ): Mono<Void> {
        response.setRawStatusCode(200)
        response.headers.contentType = MediaType.APPLICATION_JSON
        val buffer = data.map { response.bufferFactory().wrap(it) }.flux()
        return response.writeWith(buffer)
    }

    /**
     * Writes a byte array to the given ServerHttpResponse.
     *
     * @param response The ServerHttpResponse object representing the HTTP response.
     * @param data The byte array to be written as the response body.
     * @return A Mono that represents the completion of the response writing process.
     */
    open fun writeByteArray(
        response: ServerHttpResponse,
        data: ByteArray,
    ): Mono<Void> {
        val dataBuffer = response.bufferFactory().wrap(data)
        val just = Flux.just(dataBuffer)
        return response.writeWith(just)
    }
}
