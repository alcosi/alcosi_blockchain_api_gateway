package com.alcosi.nft.apigateway.service.requestHistory

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingErrorWebExceptionHandler
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingUtils
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.web.ErrorProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicLong

class RequestHistoryExceptionHandler(
    protected val dBService: RequestHistoryDBService,
    protected val requestHistoryInfoFiled: String = PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO,
    addIdHeader: Boolean,
    utils: SpringCloudGatewayLoggingUtils,
    errorAttributes: ErrorAttributes,
    resources: WebProperties.Resources,
    errorProperties: ErrorProperties,
    applicationContext: ApplicationContext,
) : SpringCloudGatewayLoggingErrorWebExceptionHandler(addIdHeader, utils, errorAttributes, resources, errorProperties, applicationContext) {
    override fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        val rsMono = super.renderErrorResponse(request)
        return rsMono.map { RequestHistoryResponse(dBService, requestHistoryInfoFiled, it) }
    }

    open class RequestHistoryResponse(
        protected val dBService: RequestHistoryDBService,
        protected val requestHistoryInfoFiled: String = PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO,
        protected val delegateRs: ServerResponse,
    ) : ServerResponse by delegateRs {
        override fun writeTo(
            exchange: ServerWebExchange,
            context: ServerResponse.Context,
        ): Mono<Void> {
            val requestHistoryInfo = exchange.attributes[requestHistoryInfoFiled]!! as RequestHistoryDBService.HistoryRqInfo
            val logged =
                exchange.mutate().response(
                    RequestHistoryResponseInterceptor(requestHistoryInfo, dBService, exchange.response),
                ).build()
            return delegateRs.writeTo(logged, context)
        }
    }

    open class RequestHistoryResponseInterceptor(
        protected val info: RequestHistoryDBService.HistoryRqInfo,
        protected val dBService: RequestHistoryDBService,
        protected val delegateRs: ServerHttpResponse,
    ) :
        ServerHttpResponseDecorator(delegateRs) {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            val deferredContentLength = AtomicLong(0)
            val countedBody =
                Flux.from(body).map { dataBuffer ->
                    deferredContentLength.addAndGet(dataBuffer.readableByteCount().toLong())
                    dataBuffer
                }
            return super.writeWith(countedBody).doOnSuccess { dBService.saveRs(info, deferredContentLength.get(), statusCode) }
        }
    }
}
