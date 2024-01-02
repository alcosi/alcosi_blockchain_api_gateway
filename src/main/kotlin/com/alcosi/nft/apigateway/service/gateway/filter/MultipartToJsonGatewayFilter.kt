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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

private val TRANSFER_ENCODING_VALUE = "chunked"

open class MultipartToJsonGatewayFilter(private val order: Int = 0) : MicroserviceGatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {

        val contentType = exchange.request.headers.contentType
        val compatibleWith = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
        if (!compatibleWith) {
            return chain.filter(exchange)
        } else {
            val changedHeadersRequest = exchange.request.mutate()
                .header(HttpHeaders.TRANSFER_ENCODING, TRANSFER_ENCODING_VALUE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, null)
                .build()
            return chain.filter(MultipartToJsonWebExchange(exchange.mutate().request(changedHeadersRequest).build()))
        }

    }

    override fun getOrder(): Int {
        return order
    }

    open class MultipartToJsonWebExchange(delegate: ServerWebExchange) : ServerWebExchangeDecorator(delegate) {
        override fun getRequest(): ServerHttpRequest {
            return MultipartToJsonRequestDecorator(delegate)
        }
    }

    open class MultipartToJsonRequestDecorator(private val exchange: ServerWebExchange) :
        ServerHttpRequestDecorator(exchange.request), Logging {


        @Suppress("UNUSED_PARAMETER")
        override fun getBody(): Flux<DataBuffer> {
            val rs = exchange.multipartData.flux().flatMap { multipartData ->
                val parts = multipartData.toSingleValueMap()
                val bt = parts.map { (name, part) ->
                    val bytes = part.content().publishOn(Schedulers.boundedElastic())
                        .reduce(ByteArray(0)) { array, buffer ->
                            val position = buffer.readPosition();
                            val contentBytes = buffer.asInputStream().readAllBytes()
                            buffer.readPosition(position)
                            array + contentBytes
                        }
                    bytes.publishOn(Schedulers.boundedElastic())
                        .map { name to if (part is FilePart) Base64.encodeBase64String(it) else String(it) }
                }
                val jsonMono = Flux.fromIterable(bt).flatMap { it }
                    .publishOn(Schedulers.boundedElastic())
                    .reduce(objectMapper.createObjectNode()!!) { acc, mono ->
                        setJsonNodeValue(mapAsNodeOrText(mono.second), acc, mono.first)
                        acc
                    }
                val dataBuffer = jsonMono
                    .publishOn(Schedulers.boundedElastic())
                    .map {
                        val writeValueAsBytes = objectMapper.writeValueAsBytes(it)
                        exchange.response.bufferFactory().wrap(writeValueAsBytes)
                    }
                dataBuffer.publishOn(Schedulers.boundedElastic()).flux()
            }
            return rs.cache();
        }

        protected fun setJsonNodeValue(
            value: Any?,
            node: ObjectNode,
            key: String
        ): ObjectNode {
            if (value != null) {
                if (value is JsonNode) {
                    node.set(key, value as JsonNode)
                } else {
                    node.put(key, value as String)
                }
            }
            return node
        }

        /**
         * returns JsonNode or text.
         */
        protected fun mapAsNodeOrText(value: String): Any {
            return try {
                val node = objectMapper.readTree(value)
                //There is bug with wrong JsonNode determination (Denominates as Double if starts with numbers and dot, trims text afterwards)
                if (node.isObject || node.isArray) {
                    node
                } else {
                    value
                }
            } catch (t: Throwable) {
                logger.warn("Error parse json:")
                value
            }
        }

        companion object {
            val objectMapper = jacksonObjectMapper()
        }
    }
}
