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

package com.alcosi.nft.apigateway.service.gateway.filter

import com.alcosi.lib.object_mapper.MappingHelper
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
    open fun <T> readBody(exchange: ServerWebExchange, clazz: Class<T>): Mono<T> {
        val map = readBody(exchange).map { mappingHelper.mapOne(String(it), clazz as Class<Any>) as T }
        return map
    }

    open fun writeJson(response: ServerHttpResponse, data: Any): Mono<Void> {
        val serialize = mappingHelper.serialize(data)
        return writeByteArrayJson(response, serialize!!.toByteArray())
    }

    open fun writeMonoJson(response: ServerHttpResponse, data: Mono<out Any>): Mono<Void> {
        return writeMonoByteArrayJson(response, data.map { mappingHelper.serialize(it)!!.toByteArray() })
    }

    open fun writeByteArrayJson(response: ServerHttpResponse, data: ByteArray): Mono<Void> {
        response.setRawStatusCode(200)
        response.headers.contentType = MediaType.APPLICATION_JSON
        val dataBuffer = response.bufferFactory().wrap(data)
        val just = Flux.just(dataBuffer)
        return response.writeWith(just)
    }

    protected fun writeMonoByteArrayJson(response: ServerHttpResponse, data: Mono<ByteArray>): Mono<Void> {
        response.setRawStatusCode(200)
        response.headers.contentType = MediaType.APPLICATION_JSON
        val buffer = data.map { response.bufferFactory().wrap(it) }.flux()
        return response.writeWith(buffer)
    }

    open fun writeByteArray(response: ServerHttpResponse, data: ByteArray): Mono<Void> {
        val dataBuffer = response.bufferFactory().wrap(data)
        val just = Flux.just(dataBuffer)
        return response.writeWith(just)
    }
}