package com.alcosi.nft.apigateway.service.gateway.filter

import com.alcosi.lib.secured.container.SecuredDataByteArray
import com.alcosi.lib.secured.container.SecuredDataString
import com.alcosi.lib.secured.encrypt.key.KeyProvider
import com.alcosi.nft.apigateway.config.PathConfig
import com.alcosi.nft.apigateway.config.PathConfig.Companion.ATTRIBUTE_CONFIG_FIELD
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.breninsul.webfluxlogging.CommonLoggingUtils
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.OpenDefaultParts
import org.springframework.http.codec.multipart.Part
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.util.MultiValueMap
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
private val TRANSFER_ENCODING_VALUE = "chunked"

open class EncryptGatewayFilter(
    val utils: CommonLoggingUtils,
    val keyProvider: KeyProvider,
    val objectMapper: ObjectMapper
) : MicroserviceGatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val contentType = exchange.request.headers.contentType
        val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
        val isJson=contentType?.includes(MediaType.APPLICATION_JSON)?: false
        val exchangeMod=if (isJson){
            val changedHeadersRequest = exchange.request.mutate()
                .header(HttpHeaders.TRANSFER_ENCODING, TRANSFER_ENCODING_VALUE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_LENGTH, null)
                .build()
             exchange.mutate().request(changedHeadersRequest).build()
        }  else{
            exchange
        }
        return chain.filter(EncryptWebExchange(exchangeMod, utils, keyProvider, objectMapper))

    }

    override fun getOrder(): Int {
        return Int.MIN_VALUE
    }

    open class EncryptWebExchange(
        delegate: ServerWebExchange,
        val utils: CommonLoggingUtils,
        val keyProvider: KeyProvider,
        val objectMapper: ObjectMapper
    ) :
        ServerWebExchangeDecorator(delegate), Logging {
        val configFields = (delegate.attributes[ATTRIBUTE_CONFIG_FIELD] as PathConfig.ProxyRouteConfig?)?.encryptFields

        override fun getRequest(): ServerHttpRequest {
            return EncryptResponseDecorator(delegate, utils, keyProvider, objectMapper)
        }

        override fun getMultipartData(): Mono<MultiValueMap<String, Part>> {
            if (configFields.isNullOrEmpty()) {
                return super.getMultipartData()
            }
            val contentType = delegate.request.headers.contentType
            val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
            if (isMultipart) {
                val key = Mono.fromCallable { keyProvider.key(KeyProvider.MODE.ENCRYPT) }
                    .subscribeOn(Schedulers.boundedElastic())
                return delegate.multipartData.map { multipartData ->
                    val fieldsToEncrypt = multipartData.keys.filter { key ->
                        configFields.any { configKey ->
                            configKey.equals(
                                key,
                                true
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
            key: Mono<ByteArray>
        ): MultiValueMap<String, Part> {
            fieldsToEncrypt.forEach { field ->
                val partsToEncrypt = multipartData[field]!!
                val encrypted = partsToEncrypt
                    .map { part ->
                        val filePartEncrypted = DataBufferUtils.join(part.content())
                            .flatMap {
                                val container =
                                    if (part is FilePart)
                                        key.flatMap { k ->
                                            Mono.fromCallable {
                                                val time = System.currentTimeMillis()
                                                val encrypted =
                                                    SecuredDataByteArray.create(utils.getContentBytes(it), k)
                                                logger.debug("Encrypt took ${System.currentTimeMillis() - time} for multipart file rq ${field} ${encrypted.originalLength} bytes")
                                                return@fromCallable encrypted
                                            }.subscribeOn(Schedulers.boundedElastic())
                                        }
                                    else
                                        key.flatMap { k ->
                                            Mono.fromCallable {
                                                val time = System.currentTimeMillis()
                                                val encrypted =
                                                    SecuredDataString.create(utils.getContent(it, Int.MAX_VALUE), k)
                                                logger.debug("Encrypt took ${System.currentTimeMillis() - time} for multipart string rq ${field} ${encrypted.originalLength} bytes")
                                                return@fromCallable encrypted
                                            }.subscribeOn(Schedulers.boundedElastic())
                                        }
                                val bytes = container.map { b ->
                                    objectMapper.writeValueAsBytes(b)
                                }
                                bytes
                            }
                            .map { bytes -> delegate.response.bufferFactory().wrap(bytes) }
                            .flux()
                        OpenDefaultParts.create(part.headers(), filePartEncrypted)
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
        val objectMapper: ObjectMapper
    ) :
        ServerHttpRequestDecorator(exchange.request), Logging {
        val configFields = (exchange.attributes[ATTRIBUTE_CONFIG_FIELD] as PathConfig.ProxyRouteConfig?)?.encryptFields
        override fun getBody(): Flux<DataBuffer> {
            if (configFields.isNullOrEmpty()) {
                return super.getBody()
            }
            val contentType = delegate.headers.contentType
            val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
            val isJson=contentType?.includes(MediaType.APPLICATION_JSON)?: false
            if (isMultipart||!isJson) {
                return super.getBody()
            } else {
                val key = Mono.fromCallable { keyProvider.key(KeyProvider.MODE.ENCRYPT) }
                    .subscribeOn(Schedulers.boundedElastic())
                val encryptedRq = DataBufferUtils.join(super.getBody())
                    .flatMap { data ->
                        key.flatMap { k ->
                            Mono.fromCallable {
                                val contentBytes = utils.getContentBytes(data)
                                val nodeTree = objectMapper.readTree(contentBytes)
                                configFields.forEach { field ->
                                    val path = field.split(".")
                                    val nodePair = path.fold(nodeTree to nodeTree) { a, b ->
                                        val parentNode = a.second
                                        val currentNode = a.second.get(b)
                                        if (currentNode == null || currentNode.isNull) {
                                            return@forEach
                                        }
                                        return@fold parentNode to currentNode
                                    }
                                    val currentNode = nodePair.second
                                    val parentNode = nodePair.first as ObjectNode
                                    if (!currentNode.isValueNode) {
                                        throw IllegalArgumentException("Field ${field} that have to be decrypted is not Value ${currentNode.nodeType.name} ")
                                    }
                                    val textVal = currentNode.asText()
                                    val time = System.currentTimeMillis()
                                    val encrypted = SecuredDataString.create(textVal, k)
                                    logger.debug("Encrypt took ${System.currentTimeMillis() - time} for rq ${encrypted.originalLength} bytes")
                                    val encryptedNode=objectMapper.valueToTree<JsonNode>(encrypted)
                                    parentNode.set<JsonNode>(field,encryptedNode)
                                }
                                return@fromCallable nodeTree
                            }
                        }.subscribeOn(Schedulers.boundedElastic())
                    }
                    .mapNotNull { node -> objectMapper.writeValueAsBytes(node) }
                    .map { bytes -> exchange.response.bufferFactory().wrap(bytes) }
                    .flux()
                return encryptedRq

            }
        }


    }
}