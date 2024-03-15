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

import com.alcosi.lib.objectMapper.MappingHelper
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

open class GatewayFilterResponseWriter(val mappingHelper: MappingHelper) : Logging {
    protected fun readBody(exchange: ServerWebExchange): Mono<ByteArray> {
        val bodyAsStringMono: Mono<ByteArray> = getBodyAsBytes(exchange.request.body)
        return bodyAsStringMono
    }

    open fun getBodyAsBytes(data: Flux<DataBuffer>): Mono<ByteArray> {
        return DataBufferUtils.join(data)
            .publishOn(Schedulers.boundedElastic())
            .map {
                it.asInputStream().readAllBytes()
            }
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> readBody(
        exchange: ServerWebExchange,
        clazz: Class<T>,
    ): Mono<T> {
        val map = readBody(exchange).map { mappingHelper.mapOne(String(it), clazz as Class<Any>) as T }
        return map
    }

    open fun writeJson(
        response: ServerHttpResponse,
        data: Any,
    ): Mono<Void> {
        val serialize = mappingHelper.serialize(data)
        return writeByteArrayJson(response, serialize!!.toByteArray())
    }

    open fun writeMonoJson(
        response: ServerHttpResponse,
        data: Mono<out Any>,
    ): Mono<Void> {
        return writeMonoByteArrayJson(response, data.map { mappingHelper.serialize(it)!!.toByteArray() })
    }

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

    protected fun writeMonoByteArrayJson(
        response: ServerHttpResponse,
        data: Mono<ByteArray>,
    ): Mono<Void> {
        response.setRawStatusCode(200)
        response.headers.contentType = MediaType.APPLICATION_JSON
        val buffer = data.map { response.bufferFactory().wrap(it) }.flux()
        return response.writeWith(buffer)
    }

    open fun writeByteArray(
        response: ServerHttpResponse,
        data: ByteArray,
    ): Mono<Void> {
        val dataBuffer = response.bufferFactory().wrap(data)
        val just = Flux.just(dataBuffer)
        return response.writeWith(just)
    }
}
