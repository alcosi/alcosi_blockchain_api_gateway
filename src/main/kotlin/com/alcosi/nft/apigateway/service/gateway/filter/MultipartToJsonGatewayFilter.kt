package com.alcosi.nft.apigateway.service.gateway.filter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

private val TRANSFER_ENCODING_VALUE = "chunked"

open class MultipartToJsonGatewayFilter(private val order: Int = 0) : MicroserviceGatewayFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val contentType = exchange.request.headers.contentType
        val compatibleWith = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
        if (!compatibleWith) {
            return chain.filter(exchange)
        } else {
            val changedHeadersRequest =
                exchange.request.mutate()
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
            val rs =
                exchange.multipartData.flux().flatMap { multipartData ->
                    val parts = multipartData.toSingleValueMap()
                    val bt =
                        parts.map { (name, part) ->
                            val bytes =
                                part.content().publishOn(Schedulers.boundedElastic())
                                    .reduce(ByteArray(0)) { array, buffer ->
                                        try {
                                            val position = buffer.readPosition()
                                            val contentBytes = buffer.asInputStream().readAllBytes()
                                            buffer.readPosition(position)
                                            array + contentBytes
                                        } finally {
                                            DataBufferUtils.release(buffer) // Ensure buffer is released
                                        }
                                    }
                            bytes.publishOn(Schedulers.boundedElastic())
                                .map { name to if (isFilePart(part)) Base64.encodeBase64String(it) else String(it) }
                        }
                    val jsonMono =
                        Flux.fromIterable(bt).flatMap { it }
                            .publishOn(Schedulers.boundedElastic())
                            .reduce(objectMapper.createObjectNode()!!) { acc, mono ->
                                setJsonNodeValue(mapAsNodeOrText(mono.second), acc, mono.first)
                                acc
                            }
                    val dataBuffer =
                        jsonMono
                            .publishOn(Schedulers.boundedElastic())
                            .map {
                                val writeValueAsBytes = objectMapper.writeValueAsBytes(it)
                                exchange.response.bufferFactory().wrap(writeValueAsBytes)
                            }
                    dataBuffer.publishOn(Schedulers.boundedElastic()).flux()
                }
            return rs.cache()
        }

        private fun isFilePart(part: Part?): Boolean {
            val isFilePart = part is FilePart
            if (isFilePart) {
                return !part?.headers()?.get("IS_JSON")?.firstOrNull().equals("true")
            } else {
                return false
            }
        }

        protected fun setJsonNodeValue(
            value: Any?,
            node: ObjectNode,
            key: String,
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

        /** returns JsonNode or text. */
        protected fun mapAsNodeOrText(value: String): Any {
            return try {
                val node = objectMapper.readTree(value)
                // There is bug with wrong JsonNode determination (Denominates as Double if starts with numbers and dot, trims text afterwards)
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