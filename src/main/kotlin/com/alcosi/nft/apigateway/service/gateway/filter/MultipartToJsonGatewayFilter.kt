package com.alcosi.nft.apigateway.service.gateway.filter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
/**
 * Value indicating the "Transfer-Encoding" header field value for chunked transfer encoding.
 *
 * The "Transfer-Encoding" header field specifies the form of encoding used to safely transfer the
 * payload body in a message. When the value is set to "chunked", the message body is sent in a series
 * of chunks where each chunk is preceded by its size in hexadecimal format followed by a carriage return
 * and line feed.
 *
 * This value is used to set the "Transfer-Encoding" header field for the chunked transfer encoding in HTTP requests
 * or responses.
 *
 */
private val TRANSFER_ENCODING_VALUE = "chunked"

/**
 * The `MultipartToJsonGatewayFilter` class is a filter for the microservice gateway that converts multipart requests to JSON requests.
 *
 * @property order The order of the filter.
 * @property objectMapper An instance of ObjectMapper for JSON conversion.
 * @constructor Creates an instance of `MultipartToJsonGatewayFilter`.
 */
open class MultipartToJsonGatewayFilter(private val order: Int = 0,protected val objectMapper: ObjectMapper) : MicroserviceGatewayFilter {
    /**
     * Filters the server web exchange based on the content type of the request.
     * If the content type is not compatible with multipart form data, the filter does nothing and proceeds to the next filter in the chain.
     * If the content type is compatible with multipart form data, the filter modifies the request headers and delegates the filtered request to a custom MultipartToJsonWebExchange
     * .
     *
     * @param exchange the server web exchange to be filtered
     * @param chain the gateway filter chain for processing the filtered exchange
     * @return a Mono that represents the filtered exchange
     */
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
            return chain.filter(MultipartToJsonWebExchange(exchange.mutate().request(changedHeadersRequest).build(),objectMapper))
        }
    }

    /**
     * Returns the order of the filter.
     *
     * @return The order of the filter as an integer.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * The `MultipartToJsonWebExchange` class is a subclass of `ServerWebExchangeDecorator` that converts a multipart request to JSON.
     *
     * @property delegate The decorated `ServerWebExchange`.
     * @property objectMapper The `ObjectMapper` used for JSON serialization.
     *
     * @constructor Creates an instance of `MultipartToJsonWebExchange`.
     * @param delegate The `ServerWebExchange` to be decorated.
     * @param objectMapper The `ObjectMapper` used for JSON serialization.
     */
    open class MultipartToJsonWebExchange(delegate: ServerWebExchange,protected val objectMapper: ObjectMapper) : ServerWebExchangeDecorator(delegate) {
        override fun getRequest(): ServerHttpRequest {
            return MultipartToJsonRequestDecorator(delegate,objectMapper)
        }
    }

    /**
     * This class is a decorator for `ServerHttpRequest` that provides additional functionality for handling multipart requests and converting them to JSON.
     * It extends the `ServerHttpRequestDecorator` class and implements the `Logging` interface.
     *
     * @param exchange The `ServerWebExchange` object representing the current server exchange.
     * @param objectMapper The `ObjectMapper` object used for JSON serialization and deserialization.
     */
    open class MultipartToJsonRequestDecorator(protected val exchange: ServerWebExchange,protected val objectMapper: ObjectMapper) : ServerHttpRequestDecorator(exchange.request), Logging {
        /**
         * Gets the body of the HTTP request as a Flux of DataBuffer objects.
         * This method is used to retrieve the body of a multipart request,
         * parse the parts, and create a JSON object from the part names and values.
         * The resulting JSON object is then converted to a byte array and wrapped
         * in a DataBuffer object.
         *
         * @return Flux<DataBuffer> - A Flux of DataBuffer objects containing the
         *         body of the HTTP request.
         */
        @Suppress("UNUSED_PARAMETER")
        override fun getBody(): Flux<DataBuffer> {
            val rs =
                exchange.multipartData.flux().flatMap { multipartData ->
                    val parts = multipartData.toSingleValueMap()
                    val bt =
                        parts.map { (name, part) ->
                            val bytes =
                                part.content().publishOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
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
                            bytes.publishOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                                .map { name to if (isFilePart(part)) Base64.encodeBase64String(it) else String(it) }
                        }
                    val jsonMono =
                        Flux.fromIterable(bt).flatMap { it }
                            .publishOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                            .reduce(objectMapper.createObjectNode()!!) { acc, mono ->
                                setJsonNodeValue(mapAsNodeOrText(mono.second), acc, mono.first)
                                acc
                            }
                    val dataBuffer =
                        jsonMono
                            .publishOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                            .map {
                                val writeValueAsBytes = objectMapper.writeValueAsBytes(it)
                                exchange.response.bufferFactory().wrap(writeValueAsBytes)
                            }
                    dataBuffer.publishOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler).flux()
                }
            return rs.cache()
        }

        /**
         * Checks if the given part is a file part.
         *
         * @param part The part to be checked.
         * @return True if the part is a file part and it is not marked as "IS_JSON", false otherwise.
         */
        protected open fun isFilePart(part: Part?): Boolean {
            val isFilePart = part is FilePart
            if (isFilePart) {
                return !part?.headers()?.get("IS_JSON")?.firstOrNull().equals("true")
            } else {
                return false
            }
        }

        /**
         * Updates the value of a specific key in the provided JSON object node.
         *
         * @param value The value to be set for the key. Can be any object.
         * @param node The JSON object node where the value will be updated.
         * @param key The key for which the value will be updated.
         * @return The updated JSON object node with the new value set for the specified key.
         */
        protected open fun setJsonNodeValue(
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

        /**
         * Maps the given value as either a JSON node or as text.
         *
         * @param value the value to be mapped
         * @return the mapped value as either a JSON node or as text
         */
        protected open fun mapAsNodeOrText(value: String): Any {
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

    }
}