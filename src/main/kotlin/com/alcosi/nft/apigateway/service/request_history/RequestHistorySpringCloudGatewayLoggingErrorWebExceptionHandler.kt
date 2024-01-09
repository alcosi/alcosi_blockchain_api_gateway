package com.alcosi.nft.apigateway.service.request_history

import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingErrorWebExceptionHandler
import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingResponseInterceptor
import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingUtils
import org.springframework.boot.autoconfigure.web.ErrorProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class RequestHistorySpringCloudGatewayLoggingErrorWebExceptionHandler(
    val requestHistoryDBComponent: RequestHistoryDBComponent,
    addIdHeader: Boolean,
     utils: SpringCloudGatewayLoggingUtils,
    errorAttributes: ErrorAttributes,
    resources: WebProperties.Resources,
    errorProperties: ErrorProperties,
    applicationContext: ApplicationContext,
): SpringCloudGatewayLoggingErrorWebExceptionHandler(addIdHeader, utils, errorAttributes, resources, errorProperties, applicationContext) {

    override fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
//        val rqTime=exchange.attributes[rqTimeFiled] as LocalDateTime
        val errorResponse = super.renderErrorResponse(request)
        errorResponse.map {
//            requestHistoryDBComponent.saveRs()
        }
        return errorResponse
    }

    open class  RequestHistoryResponseLoggingErrorInterceptor(
         addIdHeader: Boolean,
         delegateRs: ServerResponse,
         utils:SpringCloudGatewayLoggingUtils,
         startTimeAttribute:String = "startTime",
    ) : ResponseLoggingErrorInterceptor(addIdHeader, delegateRs, utils, startTimeAttribute) {
       override fun writeTo(exchange: ServerWebExchange, context: ServerResponse.Context): Mono<Void> {
            val withLog = exchange.mutate().response(
                SpringCloudGatewayLoggingResponseInterceptor(
                    addIdHeader,
                    exchange.response,
                    exchange.request,
                    exchange.attributes[startTimeAttribute] as Long?,
                    utils
                )
            ).build()
            return super.writeTo(exchange, context)
        }
    }
}