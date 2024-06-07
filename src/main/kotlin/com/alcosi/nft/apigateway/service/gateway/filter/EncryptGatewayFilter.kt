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

import com.alcosi.lib.secured.container.SecuredDataByteArray
import com.alcosi.lib.secured.container.SecuredDataString
import com.alcosi.lib.secured.encrypt.key.KeyProvider
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.breninsul.namedlimitedvirtualthreadexecutor.service.VirtualTreadExecutor
import io.github.breninsul.webfluxlogging.CommonLoggingUtils
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.EncryptedDefaultParts
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
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
 * The `EncryptGatewayFilter` class is responsible for performing encryption operations
 * on incoming requests  in a gateway filter chain.
 *
 * @param utils: The CommonLoggingUtils instance used for logging.
 * @param keyProvider: The KeyProvider instance used for obtaining encryption keys.
 * @param objectMapper: The ObjectMapper instance used for JSON serialization and deserialization.
 * @param attrProxyConfigField: The name of the attribute in the server web exchange containing the proxy route configuration field.
 * @param order: The order of the filter in the filter chain. Defaults to `Int.MIN_VALUE`.
 */
open class EncryptGatewayFilter(
    val utils: CommonLoggingUtils,
    val keyProvider: KeyProvider,
    val objectMapper: ObjectMapper,
    val attrProxyConfigField: String,
    private val order: Int = Int.MIN_VALUE,
) : MicroserviceGatewayFilter {
    /**
     * Filters the given request and response.
     *
     * @param exchange The server web exchange.
     * @param chain The gateway filter chain.
     * @return A Mono that represents the completion of the request handling.
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val contentType = exchange.request.headers.contentType
        val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
        val isJson = contentType?.includes(MediaType.APPLICATION_JSON) ?: false
        val exchangeMod =
            if (isJson) {
                val changedHeadersRequest =
                    exchange.request.mutate()
                        .header(HttpHeaders.TRANSFER_ENCODING, TRANSFER_ENCODING_VALUE)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header("${HttpHeaders.CONTENT_LENGTH}_ORIGINAL", "${exchange.request.headers.contentLength}")
                        .header(HttpHeaders.CONTENT_LENGTH, null)
                        .build()
                exchange.mutate().request(changedHeadersRequest).build()
            } else {
                exchange
            }
        return chain.filter(EncryptWebExchange(exchangeMod, utils, keyProvider, objectMapper, attrProxyConfigField))
    }

    /**
     * Retrieves the order of the EncryptGatewayFilter.
     *
     * @return The order of the EncryptGatewayFilter as an integer.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * EncryptWebExchange is a class that extends ServerWebExchangeDecorator and provides additional functionality for handling encryption of requests in a web exchange.
     *
     * @property delegate The original ServerWebExchange object.
     * @property utils The CommonLoggingUtils object used for logging.
     * @property keyProvider The KeyProvider object used for obtaining encryption keys.
     * @property objectMapper The ObjectMapper object used for serializing and deserializing objects.
     * @property attrProxyConfigField The name of the attribute in the exchange's attributes map that contains the ProxyRouteConfigDTO object.
     */
    open class EncryptWebExchange(
        delegate: ServerWebExchange,
        val utils: CommonLoggingUtils,
        val keyProvider: KeyProvider,
        val objectMapper: ObjectMapper,
        val attrProxyConfigField: String,
    ) : ServerWebExchangeDecorator(delegate), Logging {
        /**
         * Represents the configuration fields for encryption given a ProxyRouteConfigDTO object.
         * It retrieves the encryptFields property from the ProxyRouteConfigDTO object,
         * which contains a list of fields to encrypt in the request.
         * If the ProxyRouteConfigDTO object or the encryptFields property is null,
         * it returns null, indicating no fields need to be encrypted.
         *
         * @see ProxyRouteConfigDTO
         */
        val configFields = (delegate.attributes[attrProxyConfigField] as ProxyRouteConfigDTO?)?.encryptFields

        /**
         * Returns a new instance of [ServerHttpRequest] that is decorated with encryption functionality.
         *
         * @return The decorated [ServerHttpRequest].
         */
        override fun getRequest(): ServerHttpRequest {
            return EncryptResponseDecorator(delegate, utils, keyProvider, objectMapper, attrProxyConfigField)
        }

        /**
         * Retrieves multi-part data from the request.
         *
         * @return A Mono emitting a MultiValueMap containing the multi-part data.
         */
        override fun getMultipartData(): Mono<MultiValueMap<String, Part>> {
            if (configFields.isNullOrEmpty()) {
                return super.getMultipartData()
            }
            val contentType = delegate.request.headers.contentType
            val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
            if (isMultipart) {
                val key =
                    Mono.fromFuture(CompletableFuture.supplyAsync({ keyProvider.key(KeyProvider.MODE.ENCRYPT) }, executor))
                        .subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                        .cache()
                return delegate.multipartData.map { multipartData ->
                    val fieldsToEncrypt =
                        multipartData.keys.filter { key ->
                            configFields.any { configKey ->
                                configKey.equals(
                                    key,
                                    true,
                                )
                            }
                        }
                    if (fieldsToEncrypt.isEmpty()) {
                        return@map multipartData
                    } else {
                        encodeMultiPart(fieldsToEncrypt, multipartData, key)
                        return@map multipartData
                    }
                }
            } else {
                return super.getMultipartData()
            }
        }

        /**
         * Encodes the multi-part data by encrypting specified fields using the provided key.
         *
         * @param fieldsToEncrypt The list of field names to encrypt.
         * @param multipartData The MultiValueMap containing the multi-part data.
         * @param key The key used for encryption. This is a Mono that emits a ByteArray.
         * @return The modified MultiValueMap with encrypted parts.
         */
        protected open fun encodeMultiPart(
            fieldsToEncrypt: List<String>,
            multipartData: MultiValueMap<String, Part>,
            key: Mono<ByteArray>,
        ): MultiValueMap<String, Part> {
            fieldsToEncrypt.forEach { field ->
                val partsToEncrypt = multipartData[field]!!
                if (partsToEncrypt.size > 1) {
                    logger.debug("Encrypt several parts for $field ${partsToEncrypt.size}")
                }
                if (partsToEncrypt.all { part -> part is EncryptedDefaultParts.EncryptedPart }) {
                    logger.debug("Encrypt parts $field already has been encrypted")
                    return@forEach
                }
                val encrypted =
                    partsToEncrypt
                        .map { part ->
                            if (part is EncryptedDefaultParts.EncryptedPart) {
                                logger.debug("Encrypt part $field already has been encrypted")
                                return@map part
                            }
                            val filePartEncrypted =
                                DataBufferUtils.join(part.content())
                                    .flatMap { dataBuffer ->
                                        Mono.using(
                                            { dataBuffer },
                                            { buffer ->
                                                val container =
                                                    if (part is FilePart) {
                                                        key.flatMap { k ->
                                                            Mono.fromFuture(
                                                                CompletableFuture.supplyAsync({
                                                                    val time = System.currentTimeMillis()
                                                                    val encrypted = SecuredDataByteArray.create(utils.getContentBytes(buffer) ?: "".toByteArray(), k)
                                                                    logger.info("Encrypt took ${System.currentTimeMillis() - time} for multipart file rq $field ${encrypted.originalLength} bytes")
                                                                    return@supplyAsync encrypted
                                                                }, executor)
                                                            ).subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                                                        }
                                                    } else {
                                                        key.flatMap { k ->
                                                            Mono.fromFuture(
                                                                CompletableFuture.supplyAsync({
                                                                    val time = System.currentTimeMillis()
                                                                    val encrypted = SecuredDataString.create(utils.getContent(buffer, Int.MAX_VALUE) ?: "", k)
                                                                    logger.info("Encrypt took ${System.currentTimeMillis() - time} for multipart string rq $field ${encrypted.originalLength} bytes")
                                                                    return@supplyAsync encrypted
                                                                }, executor)
                                                            ).subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                                                        }
                                                    }
                                                val bytes = container.map { b -> objectMapper.writeValueAsBytes(b) }
                                                bytes.map { byteArray -> delegate.response.bufferFactory().wrap(byteArray) }
                                            },
                                            { buffer -> DataBufferUtils.release(buffer) } // Ensure buffer is released
                                        )
                                    }
                                    .flux()
                                    .cache()
                            EncryptedDefaultParts.create(part.headers(), filePartEncrypted)
                        }
                multipartData[field] = encrypted
            }
            return multipartData
        }
    }

    /**
     * EncryptResponseDecorator is a class that decorates the ServerHttpRequest in order to encrypt specific fields in the response.
     *
     * @param exchange The ServerWebExchange object representing the current request and response.
     * @param utils An instance of the CommonLoggingUtils class used for logging purposes.
     * @param keyProvider An instance of the KeyProvider class used for retrieving encryption keys.
     * @param objectMapper An instance of the ObjectMapper class used for serializing and deserializing JSON.
     * @param attrProxyConfigField The attribute name for retrieving ProxyRouteConfigDTO from the exchange attributes.
     */
    open class EncryptResponseDecorator(
        protected val exchange: ServerWebExchange,
        val utils: CommonLoggingUtils,
        val keyProvider: KeyProvider,
        val objectMapper: ObjectMapper,
        val attrProxyConfigField: String,
    ) : ServerHttpRequestDecorator(exchange.request), Logging {
        /**
         * Represents a list of fields to be encrypted in the request.
         */
        val configFields = (exchange.attributes[attrProxyConfigField] as ProxyRouteConfigDTO?)?.encryptFields

        /**
         * Returns the body of the response.
         *
         * If the configFields list is null or empty, the method calls the superclass's getBody() method and returns the result.
         *
         * If the content type of the headers is multipart or not JSON, the method calls the superclass's getBody() method and returns the result.
         *
         * If the content type is JSON and there are fields to encrypt, the method performs the following steps:
         * 1. Retrieves the encryption key by asynchronously computing it using the keyProvider.
         * 2. Caches the encryption key.
         * 3. Joins the DataBuffers of the superclass's getBody() method into a single DataBuffer.
         * 4. Encrypts the joined data buffer.
         * 5. Converts the encrypted JSON node to bytes.
         * 6. Wraps the bytes in a DataBuffer.
         * 7. Returns a Flux of the encrypted request body.
         *
         * @return A Flux of DataBuffers representing the body of the response.
         */
        override fun getBody(): Flux<DataBuffer> {
            if (configFields.isNullOrEmpty()) {
                return super.getBody()
            }
            val contentType = delegate.headers.contentType
            val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
            val isJson = contentType?.includes(MediaType.APPLICATION_JSON) ?: false
            if (isMultipart || !isJson) {
                return super.getBody()
            } else {
                val key = Mono.fromFuture(CompletableFuture.supplyAsync({ keyProvider.key(KeyProvider.MODE.ENCRYPT) }, executor))
                    .subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                    .cache()
                val encryptedRq = DataBufferUtils.join(super.getBody())
                    .flatMap { dataBuffer ->
                        Mono.using(
                            { dataBuffer },
                            { buffer ->
                                key.flatMap { k ->
                                    Mono.fromFuture(
                                        CompletableFuture.supplyAsync({
                                            val contentBytes = utils.getContentBytes(buffer)
                                            val nodeTree = objectMapper.readTree(contentBytes)
                                            configFields.forEach { field ->
                                                val path = field.split(".")
                                                encryptPath(nodeTree, path, k)
                                            }
                                            return@supplyAsync nodeTree
                                        }, executor)
                                    )
                                }.subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                            },
                            { buffer -> DataBufferUtils.release(buffer) }// Ensure buffer is released
                        )
                    }
                    .mapNotNull { node -> objectMapper.writeValueAsBytes(node) }
                    .map { bytes -> exchange.response.bufferFactory().wrap(bytes) }
                    .flux()
                    .cache()
                return encryptedRq
            }
        }

        /**
         * Encrypts the specified path in the given JSON node using the encryption key.
         *
         * @param currentNode The current JSON node.
         * @param path The path to encrypt.
         * @param encryptionKey The encryption key to use.
         */
        protected open fun encryptPath(currentNode: JsonNode, path: List<String>, encryptionKey: ByteArray) {
            var node: JsonNode = currentNode
            var parentNode = node
            for ((index, part) in path.withIndex()) {
                if (node.isObject) {
                    parentNode = node
                    node = node.get(part) ?: return  // Path not found
                } else if (node.isArray) {
                    for (element in node) {
                        if (element.isObject || element.isArray) {
                            encryptPath(element, path.subList(index, path.size), encryptionKey)
                        }
                    }
                    return
                } else {
                    return
                }
            }
            if (node.isValueNode && parentNode is ObjectNode) {
                node = objectMapper.valueToTree(encodeNode(node, encryptionKey))
                parentNode.replace(path.last(), node)
            }
        }

        /**
         * Encodes the specified JSON node using the encryption key.
         *
         * @param node The JSON node to be encoded.
         * @param encryptionKey The encryption key to use.
         * @return The encoded JSON node as a [SecuredDataString] object.
         */
        protected open fun encodeNode(
            node: JsonNode,
            encryptionKey: ByteArray
        ): SecuredDataString {
            val time = System.currentTimeMillis()
            val encrypted = SecuredDataString.create(node.asText(), encryptionKey)
            logger.info("Encrypt took ${System.currentTimeMillis() - time} for rq ${encrypted.originalLength} bytes")
            return encrypted
        }

    }

    /**
     * The Companion class represents the companion object of a class. In Kotlin, the companion object is declared inside the class and can contain properties and methods that are
     *  related to the class itself.
     *
     * @property executor The executor used by the class. It is protected and can be accessed by subclasses.
     */
    companion object {
        /**
         * An open protected property representing the executor for running tasks.
         * By default, it creates a new `VirtualThreadPerTaskExecutor`.
         */
        protected open val executor = VirtualTreadExecutor
    }
}