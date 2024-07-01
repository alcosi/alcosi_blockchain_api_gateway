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

import com.alcosi.lib.secured.encrypt.SensitiveComponent
import com.alcosi.lib.secured.encrypt.key.KeyProvider
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import io.github.breninsul.namedlimitedvirtualthreadexecutor.service.VirtualTreadExecutor
import io.github.breninsul.webfluxlogging.CommonLoggingUtils
import org.apache.logging.log4j.kotlin.Logging
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

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
private val TRANSFER_ENCODING_CHUNKED_VALUE = "chunked"

/**
 * DecryptGatewayFilter is a class that implements the MicroserviceGatewayFilter interface.
 * It provides a filter method that is responsible for decrypting the response body of a ServerWebExchange.
 * The decryption is done using a SensitiveComponent and a KeyProvider.
 * The decrypted response is then passed to the next filter in the GatewayFilterChain.
 *
 * @property utils The CommonLoggingUtils instance used for logging.
 * @property sensitiveComponent The SensitiveComponent used for decryption.
 * @property keyProvider The KeyProvider used for obtaining the decryption key.
 * @property order The order of the filter. Default is Int.MIN_VALUE.
 */
open class DecryptGatewayFilter(
    val utils: CommonLoggingUtils,
    val sensitiveComponent: SensitiveComponent,
    val keyProvider: KeyProvider,
    private val order: Int = Int.MIN_VALUE,
    val attrProxyConfigField: String,
    ) : MicroserviceGatewayFilter {
    /**
     * Applies the filter to the given server web exchange and gateway filter chain.
     *
     * @param exchange the server web exchange
     * @param chain the gateway filter chain
     * @return a Mono representing the completion of the filter
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val decorated = exchange.mutate().response(DecryptResponseDecorator(exchange, utils, sensitiveComponent, keyProvider,attrProxyConfigField)).build()

        return chain.filter(decorated)
    }

    /**
     * Returns the order of this DecryptGatewayFilter.
     *
     * @return The order of the DecryptGatewayFilter.
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * DecryptResponseDecorator is a class that extends ServerHttpResponseDecorator and is responsible
     * for decrypting the response body before it is sent to the client. It decrypts the content using
     * a provided key obtained from a KeyProvider and a SensitiveComponent for decryption.
     *
     * @param exchange The ServerWebExchange object representing the current HTTP exchange.
     * @param utils An instance of CommonLoggingUtils for logging purposes.
     * @param sensitiveComponent An instance of SensitiveComponent for performing the decryption.
     * @param keyProvider An instance of KeyProvider for obtaining the decryption key.
     */
    open class DecryptResponseDecorator(
        protected val exchange: ServerWebExchange,
        val utils: CommonLoggingUtils,
        val sensitiveComponent: SensitiveComponent,
        val keyProvider: KeyProvider,
        val attrProxyConfigField: String,
        ) : ServerHttpResponseDecorator(exchange.response), Logging {
        /**
         * Writes the given body to the response, performing decryption and other necessary operations.
         *
         * @param body The body to write to the response.
         * @return A Mono representing the completion of the write operation.
         */
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            val decrypt = (exchange.attributes[attrProxyConfigField] as ProxyRouteConfigDTO?)?.decryptResponse?:false
            if (!decrypt){
                return super.writeWith(body)
            }
            return DataBufferUtils.join(body)
                .flatMap { dataBuffer ->
                    val content = utils.getContentBytes(dataBuffer)?.let { dt -> String(dt) }
                    DataBufferUtils.release(dataBuffer) // Ensure buffer is released
                    if (content == null) {
                        return@flatMap Mono.empty()
                    }
                    val key = Mono.fromFuture {
                        val time = System.currentTimeMillis()
                        val k = CompletableFuture.supplyAsync({ keyProvider.key(KeyProvider.MODE.DECRYPT) }, executor)
                        logger.info("Key getting took ${System.currentTimeMillis() - time}")
                        return@fromFuture k
                    }
                        .subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                        .cache()
                    val decrypted = key.mapNotNull { k ->
                        val d = Mono.fromFuture(CompletableFuture.supplyAsync({
                            val time = System.currentTimeMillis()
                            val decrypted = sensitiveComponent.decrypt(content, k)
                            logger.info("Decrypt took ${System.currentTimeMillis() - time} for rs ${decrypted?.length} bytes")
                            return@supplyAsync decrypted
                        }, executor))
                        return@mapNotNull d.mapNotNull { dd -> dd?.toByteArray() }
                    }
                        .flatMap { d -> d }
                        .subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
                    return@flatMap decrypted
                }
                .mapNotNull { decrypted ->
                    if ((headers[HttpHeaders.TRANSFER_ENCODING]?.firstOrNull() ?: "") != TRANSFER_ENCODING_CHUNKED_VALUE) {
                        this.headers.set(HttpHeaders.CONTENT_LENGTH, "${decrypted!!.size}")
                    }
                    return@mapNotNull exchange.response.bufferFactory().wrap(decrypted!!)
                }
                .cache()
                .flatMap { dataBuffer ->
                    super.writeWith(Mono.just(dataBuffer))
                }

        }
    }

    /**
     * Companion object for the DecryptGatewayFilter class.
     */
    companion object {
        /**
         * The executor variable is a protected property that holds an instance of an executor.
         *
         * It is created using the newVirtualThreadPerTaskExecutor() method from the Executors class, which returns an implementation of the Executor interface.
         * This executor creates a new thread for each task that is submitted to it, allowing tasks to run concurrently.
         *
         * Being a protected property, it can be accessed by subclasses of the current class, but not by classes outside the class hierarchy.
         *
         * @see Executors
         * @see java.util.concurrent.Executor
         */
        protected open val executor = VirtualTreadExecutor
    }
}