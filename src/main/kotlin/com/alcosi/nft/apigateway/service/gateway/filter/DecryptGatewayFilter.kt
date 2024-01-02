package com.alcosi.nft.apigateway.service.gateway.filter

import com.alcosi.lib.secured.encrypt.SensitiveComponent
import com.alcosi.lib.secured.encrypt.key.KeyProvider
import com.github.breninsul.webfluxlogging.CommonLoggingUtils
import org.apache.logging.log4j.kotlin.Logging
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

open class DecryptGatewayFilter(
    val utils: CommonLoggingUtils,
    val sensitiveComponent: SensitiveComponent,
    val keyProvider: KeyProvider,
    private val order: Int = Int.MIN_VALUE,
) : MicroserviceGatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val decorated=exchange.mutate().response(DecryptResponseDecorator(exchange, utils, sensitiveComponent, keyProvider)).build()

        return chain.filter(decorated)

    }

    override fun getOrder(): Int {
        return order
    }

    open class DecryptResponseDecorator(
        protected val exchange: ServerWebExchange,
        val utils: CommonLoggingUtils,
        val sensitiveComponent: SensitiveComponent,
        val keyProvider: KeyProvider
    ) :
        ServerHttpResponseDecorator(exchange.response), Logging {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            val buffer = DataBufferUtils.join(body)
                .flatMap {
                    val content = utils.getContentBytes(it)?.let { dt -> String(dt) }
                    if (content == null) {
                        return@flatMap Mono.empty()
                    }
                    val key = Mono.fromCallable {
                        val time = System.currentTimeMillis()
                        val k = keyProvider.key(KeyProvider.MODE.DECRYPT)
                        logger.debug("Key getting took ${System.currentTimeMillis() - time}")
                        return@fromCallable k

                    }
                        .subscribeOn(Schedulers.boundedElastic())
                    val decrypted = key.mapNotNull { k ->
                        val time = System.currentTimeMillis()
                        val d = sensitiveComponent.decrypt(content, k)
                        logger.debug("Decrypt took ${System.currentTimeMillis() - time} for rs ${d?.length} bytes")
                        return@mapNotNull d?.toByteArray()
                    }
                        .subscribeOn(Schedulers.boundedElastic())
                    return@flatMap decrypted
                }
                .mapNotNull { decrypted ->
                    val dataBuffer = exchange.response.bufferFactory().wrap(decrypted!!)
                    return@mapNotNull dataBuffer
                }
            return super.writeWith(buffer)
        }

    }
}