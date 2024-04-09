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
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

private val TRANSFER_ENCODING_VALUE = "chunked"

open class EncryptGatewayFilter(
    val utils: CommonLoggingUtils,
    val keyProvider: KeyProvider,
    val objectMapper: ObjectMapper,
    val attrProxyConfigField: String,
    private val order: Int = Int.MIN_VALUE,
) : MicroserviceGatewayFilter {
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

    override fun getOrder(): Int {
        return order
    }

    open class EncryptWebExchange(
        delegate: ServerWebExchange,
        val utils: CommonLoggingUtils,
        val keyProvider: KeyProvider,
        val objectMapper: ObjectMapper,
        val attrProxyConfigField: String,
    ) :
        ServerWebExchangeDecorator(delegate), Logging {
        val configFields = (delegate.attributes[attrProxyConfigField] as ProxyRouteConfigDTO?)?.encryptFields

        override fun getRequest(): ServerHttpRequest {
            return EncryptResponseDecorator(delegate, utils, keyProvider, objectMapper, attrProxyConfigField)
        }

        override fun getMultipartData(): Mono<MultiValueMap<String, Part>> {
            if (configFields.isNullOrEmpty()) {
                return super.getMultipartData()
            }
            val contentType = delegate.request.headers.contentType
            val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
            if (isMultipart) {
                val key =
                    Mono.fromCallable { keyProvider.key(KeyProvider.MODE.ENCRYPT) }
                        .subscribeOn(Schedulers.boundedElastic())
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
                                    .flatMap {
                                        val container =
                                            if (part is FilePart) {
                                                key.flatMap { k ->
                                                    Mono.fromFuture(
                                                        CompletableFuture.supplyAsync({
                                                            val time = System.currentTimeMillis()
                                                            val encrypted = SecuredDataByteArray.create(utils.getContentBytes(it) ?: "".toByteArray(), k)
                                                            logger.debug("Encrypt took ${System.currentTimeMillis() - time} for multipart file rq $field ${encrypted.originalLength} bytes")
                                                            return@supplyAsync encrypted
                                                        }, executor)
                                                    ).subscribeOn(Schedulers.boundedElastic())
                                                }
                                            } else {
                                                key.flatMap { k ->
                                                    Mono.fromFuture(
                                                        CompletableFuture.supplyAsync({
                                                            val time = System.currentTimeMillis()
                                                            val encrypted = SecuredDataString.create(utils.getContent(it, Int.MAX_VALUE) ?: "", k)
                                                            logger.debug("Encrypt took ${System.currentTimeMillis() - time} for multipart string rq $field ${encrypted.originalLength} bytes")
                                                            return@supplyAsync encrypted
                                                        }, executor)
                                                    )
                                                        .subscribeOn(Schedulers.boundedElastic())
                                                }
                                            }
                                        val bytes =
                                            container.map { b ->
                                                objectMapper.writeValueAsBytes(b)
                                            }
                                        bytes
                                    }
                                    .map { bytes -> delegate.response.bufferFactory().wrap(bytes) }
                                    .flux()
                            EncryptedDefaultParts.create(part.headers(), filePartEncrypted)
                        }
                multipartData[field] = encrypted
            }
            return multipartData
        }
    }

    open class EncryptResponseDecorator(
        protected val exchange: ServerWebExchange,
        val utils: CommonLoggingUtils,
        val keyProvider: KeyProvider,
        val objectMapper: ObjectMapper,
        val attrProxyConfigField: String,
    ) :
        ServerHttpRequestDecorator(exchange.request), Logging {
        val configFields = (exchange.attributes[attrProxyConfigField] as ProxyRouteConfigDTO?)?.encryptFields

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
                val key = Mono.fromCallable { keyProvider.key(KeyProvider.MODE.ENCRYPT) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .cache()
                val encryptedRq = DataBufferUtils.join(super.getBody())
                    .flatMap { data ->
                        key.flatMap { k ->
                            Mono.fromFuture(
                                CompletableFuture.supplyAsync({
                                    val contentBytes = utils.getContentBytes(data)
                                    val nodeTree = objectMapper.readTree(contentBytes)
                                    configFields.forEach { field ->
                                        val path = field.split(".")
                                        encryptPath(nodeTree, path, k)
                                    }
                                    return@supplyAsync nodeTree
                                }, executor)
                            )
                        }.subscribeOn(Schedulers.boundedElastic())
                    }
                    .mapNotNull { node -> objectMapper.writeValueAsBytes(node) }
                    .map { bytes -> exchange.response.bufferFactory().wrap(bytes) }
                    .flux()
                return encryptedRq
            }
        }

        private fun encryptPath(currentNode: JsonNode, path: List<String>, encryptionKey: ByteArray) {
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
                node = objectMapper.valueToTree<JsonNode>(encodeNode(node, encryptionKey))
                parentNode.replace(path.last(), node)
            }
        }

        protected open fun encodeNode(
            node: JsonNode,
            encryptionKey: ByteArray
        ): SecuredDataString {
            val time = System.currentTimeMillis()
            val encrypted = SecuredDataString.create(node.asText(), encryptionKey)
            logger.debug("Encrypt took ${System.currentTimeMillis() - time} for rq ${encrypted.originalLength} bytes")
            return encrypted
        }

    }

    companion object {
        protected open val executor = Executors.newVirtualThreadPerTaskExecutor()
    }
}
