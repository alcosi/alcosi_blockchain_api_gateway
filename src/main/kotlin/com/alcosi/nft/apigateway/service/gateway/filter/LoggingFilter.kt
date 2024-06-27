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

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import io.github.breninsul.webfluxlogging.CommonLoggingUtils
import io.github.breninsul.webfluxlogging.cloud.*
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.web.ErrorProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.context.ApplicationContext
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.logging.Level
import java.util.logging.Logger

/**
 * LoggingFilter class is responsible for logging request and response information in a Spring Cloud Gateway.
 *
 * @param addIdHeader Boolean flag indicating whether to add the request ID header
 * @param rawUtils The rawUtils instance of the Utils class for logging purposes
 * @param orderValue The order value for the filter
 * @param idHeader The name of the request ID header
 * @param startTimeAttribute The attribute key for storing the start time of the request
 */
open class LoggingFilter(
    addIdHeader: Boolean,
    protected open val rawUtils: Utils,
    orderValue: Int = Int.MIN_VALUE,
    idHeader: String = "X-Request-Id",
    startTimeAttribute: String = "startTime",
) : SpringCloudGatewayLoggingFilter(addIdHeader, rawUtils, orderValue, idHeader, startTimeAttribute) {
    /**
     * This method filters the incoming server web exchange and applies logging and modification to the request and response.
     *
     * @param exchange The ServerWebExchange object representing the incoming request and response.
     * @param chain The GatewayFilterChain object representing the filter chain.
     * @return A Mono<Void> representing the filtered exchange. It completes when the filter chain is fully processed.
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        exchange.attributes[startTimeAttribute] = System.currentTimeMillis()
        val proxyConfig = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD] as ProxyRouteConfigDTO?
        val logBody = proxyConfig?.logBody ?: true
        val logHeaders = proxyConfig?.logHeaders ?: true
        val withId =
            if (addIdHeader) {
                exchange.request
                    .mutate()
                    .header(idHeader, exchange.request.id)
                    .build()
            } else {
                exchange.request
            }
        val contentType = withId.headers.contentType
        val isMultipart = contentType?.includes(MediaType.MULTIPART_FORM_DATA) ?: false
        val requestWithFilter = RequestInterceptor(logBody, logHeaders, withId, rawUtils)
        val responseWithFilter =
            ResponseInterceptor(
                logBody,
                logHeaders,
                addIdHeader,
                exchange.response,
                withId,
                exchange.attributes[startTimeAttribute] as Long?,
                rawUtils,
            )
        val loggingWebExchange =
            exchange
                .mutate()
                .request(requestWithFilter)
                .response(responseWithFilter)
                .build()
        if (isMultipart) {
            val log =
                exchange.multipartData.flatMap { if (proxyConfig?.logBody != false) utils.getPartsContent(it) else Mono.just("<MASKED>") }.doOnNext { data ->
                    rawUtils.writeRequest(withId, if (proxyConfig?.logHeaders != false) withId.headers.asString() else "<MASKED>", data)
                }
            return log.and(chain.filter(loggingWebExchange))
        }
        return chain.filter(loggingWebExchange)
    }

    /**
     * Utils is a utility class that provides logging functionality for server gateway.
     * It extends the SpringCloudGatewayLoggingUtils class.
     *
     * @param maxBodySize The maximum size of the request/response body to be logged.
     * @param logger The logger to be used for logging.
     * @param loggingLevel The logging level for the logger.
     * @param logTime Flag to indicate whether to log the time taken for the request/response.
     * @param logHeaders Flag to indicate whether to log the request/response headers.
     * @param logBody Flag to indicate whether to log the request/response body.
     * @param commonUtils The common utilities object.
     */
    open class Utils(
        maxBodySize: Int,
        logger: Logger,
        loggingLevel: Level,
        logTime: Boolean,
        logHeaders: Boolean,
        logBody: Boolean,
        commonUtils: CommonLoggingUtils = CommonLoggingUtils(),
    ) : SpringCloudGatewayLoggingUtils(maxBodySize, logger, loggingLevel, logTime, logHeaders, logBody, commonUtils) {
        open fun writeRequest(
            request: ServerHttpRequest,
            headers: String?,
            data: String?,
        ) {
            val headersString =
                if (logHeaders) ("\n=Headers      : $headers") else ""
            val bodyString =
                if (logBody) ("\n=Body         : ${commonUtils.getBodyContent(data, maxBodySize)}") else ""
            val logString =
                """
                ===========================SERVER Gateway request begin===========================
                =ID           : ${request.id}
                =URI          : ${request.method} ${getParams(request)}$headersString$bodyString
                ===========================SERVER Gateway request end   ==========================
                """.trimIndent()

            log(logString)
        }

        /**
         * Writes a server request to the log.
         *
         * @param request The ServerHttpRequest object representing the request.
         * @param headers The headers of the request as a string.
         * @param data The data buffer of the request.
         */
        open fun writeRequest(
            request: ServerHttpRequest,
            headers: String?,
            data: DataBuffer?,
        ) {
            writeRequest(request, headers, commonUtils.getBodyContent(data, maxBodySize))
        }

        /**
         * Writes a server response to the log.
         *
         * @param request The ServerHttpRequest object representing the request.
         * @param response The ServerHttpResponse object representing the response.
         * @param headers The headers of the response as a string.
         * @param data The data buffer of the response as a string.
         * @param startTime The start time of the response in milliseconds.
         */
        open fun writeResponse(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            headers: String?,
            data: String?,
            startTime: Long?,
        ) {
            val timeString =
                if (logTime && startTime != null) ("\n=Took         : ${System.currentTimeMillis() - startTime} ms") else ""
            val headersString =
                if (logHeaders) ("\n=Headers      : $headers") else ""
            val bodyString =
                if (logBody) ("\n=Body         : ${commonUtils.getBodyContent(data, maxBodySize)}") else ""
            val logString =
                """

                ===========================SERVER Gateway response begin===========================
                =ID           : ${request.id}
                =URI          : ${request.method}  ${response.statusCode?.value()} ${getParams(request)}$timeString$headersString$bodyString
                ===========================SERVER Gateway response end   ==========================
                """.trimIndent()
            log(logString)
        }

        /**
         * Writes a server response to the log.
         *
         * @param request The ServerHttpRequest object representing the request.
         * @param response The ServerHttpResponse object representing the response.
         * @param headers The headers of the response as a string.
         * @param data The data buffer of the response as a string.
         * @param startTime The start time of the response in milliseconds.
         */
        open fun writeResponse(
            request: ServerHttpRequest,
            response: ServerHttpResponse,
            headers: String?,
            data: DataBuffer?,
            startTime: Long?,
        ) {
            writeResponse(request, response, headers, commonUtils.getBodyContent(data, maxBodySize), startTime)
        }
    }

    /**
     * RequestInterceptor is an open class that extends SpringCloudGatewayLoggingRequestInterceptor.
     * It is responsible for intercepting the incoming request and logging the request details.
     *
     * @property logBody A boolean value indicating whether to log the request body.
     * @property logHeaders A boolean value indicating whether to log the request headers.
     * @property delegateRq The ServerHttpRequest object representing the incoming request.
     * @property loggingUtilsRaw The Utils object for logging the request details.
     */
    open class RequestInterceptor(
        protected open val logBody: Boolean,
        protected open val logHeaders: Boolean,
        delegateRq: ServerHttpRequest,
        protected open val loggingUtilsRaw: Utils,
    ) : SpringCloudGatewayLoggingRequestInterceptor(delegateRq, loggingUtilsRaw) {
        /**
         * Returns the body of the request as a Flux of DataBuffers.
         *
         * @return The body of the request as a Flux of DataBuffers.
         */
        override fun getBody(): Flux<DataBuffer> {
            val headers = if (logHeaders) delegateRq.headers.asString() else "<MASKED>"
            val flux =
                DataBufferUtils
                    .join(
                        super
                            .getBody(),
                    ).publishOn(Schedulers.boundedElastic())
                    .switchIfEmpty(
                        Mono.defer {
                            loggingUtilsRaw.writeRequest(delegateRq, headers, null as String?)
                            Mono.empty<DataBuffer>()
                        },
                    ).doOnNext { dataBuffer: DataBuffer ->
                        try {
                            if (logBody) {
                                loggingUtilsRaw.writeRequest(delegateRq, headers, dataBuffer)
                            } else {
                                loggingUtilsRaw.writeRequest(delegateRq, headers, "<MASKED>")
                            }
                        } catch (e: Throwable) {
                            loggingUtils.log("Error in request filter", e)
                        }
                    }.flux()
            val cached = flux.cache()
            cached.subscribeOn(Schedulers.boundedElastic()).subscribe()
            return cached
//            return if (delegateRq.method == HttpMethod.GET) {
//                val cached = flux.cache()
//                cached.subscribeOn(Schedulers.boundedElastic()).subscribe()
//                cached
//            } else {
//                flux
//            }
        }
    }

    /**
     * ResponseInterceptor is a class that intercepts the server response and performs logging and modification.
     *
     * @property logBody Flag to indicate whether to log the response body.
     * @property logHeaders Flag to indicate whether to log the response headers.
     * @property addIdHeader Flag to indicate whether to add an ID header to the response.
     * @property delegateRs The ServerHttpResponse object representing the response.
     * @property delegateRq The ServerHttpRequest object representing the request.
     * @property startTime The start time of the response in milliseconds.
     * @property rawUtils The Utils object used for logging and utility functions.
     * @property idHeader The header name to be used for the ID header.
     */
    open class ResponseInterceptor(
        protected open val logBody: Boolean,
        protected open val logHeaders: Boolean,
        addIdHeader: Boolean,
        delegateRs: ServerHttpResponse,
        delegateRq: ServerHttpRequest,
        startTime: Long?,
        protected val rawUtils: Utils,
        idHeader: String = "X-Request-Id",
    ) : SpringCloudGatewayLoggingResponseInterceptor(addIdHeader, delegateRs, delegateRq, startTime, rawUtils, idHeader) {
        /**
         * Overrides the writeWith method from the parent class.
         *
         * @param body The Publisher object representing the data buffer of the request
         * @return A Mono object representing completion of the method
         */
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            if (addIdHeader) {
                delegateRs.headers.add(idHeader, delegateRq.id)
            }
            val headers = if (logHeaders) delegateRs.headers.asString() else "<MASKED>"
            if (!logBody) {
                rawUtils.writeResponse(delegateRq, delegateRs, headers, "<MASKED>", startTime)
            }
            val buffer = DataBufferUtils.join(body)
            val dataBufferFlux =
                buffer
                    .publishOn(Schedulers.boundedElastic())
                    .switchIfEmpty(
                        Mono.defer {
                            rawUtils.writeResponse(delegateRq, delegateRs, headers, null as DataBuffer?, startTime)
                            return@defer Mono.empty<DataBuffer>()
                        } as Mono<DataBuffer>,
                    ).doOnNext { dataBuffer: DataBuffer ->
                        try {
                            rawUtils.writeResponse(delegateRq, delegateRs, headers, dataBuffer, startTime)
                        } catch (e: Throwable) {
                            utils.log("Error in response filter", e)
                        }
                    }
            return super.writeWith(dataBufferFlux)
        }
    }

    /**
     * ErrorHandler is a class that handles errors and renders error responses for the server gateway.
     *
     * @param addIdHeader Flag to indicate whether to add an ID header to the error response.
     * @param rawUtils The Utils object used for logging functionality.
     * @param errorAttributes The ErrorAttributes object containing error attributes.
     * @param resources The WebProperties.Resources object for server resources.
     * @param errorProperties The ErrorProperties object for error properties.
     * @param applicationContext The ApplicationContext object representing the application context.
     */
    open class ErrorHandler(
        protected open val addIdHeader: Boolean,
        protected open val rawUtils: Utils,
        errorAttributes: ErrorAttributes,
        resources: WebProperties.Resources,
        errorProperties: ErrorProperties,
        applicationContext: ApplicationContext,
    ) : DefaultErrorWebExceptionHandler(errorAttributes, resources, errorProperties, applicationContext) {
        protected open val acceptsTextHtml=RequestPredicate { false }
        /**
         * Renders an error response for the given ServerRequest.
         *
         * @param request The ServerRequest object representing the request.
         * @return A Mono that emits a ServerResponse object.
         */
        override fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
            val rsMono = super.renderErrorResponse(request)
            return rsMono.map { ErrorInterceptor(addIdHeader, it, rawUtils) }
        }

        override fun acceptsTextHtml(): RequestPredicate {
            return acceptsTextHtml
        }
    }

    /**
     * ErrorInterceptor is an open class that intercepts errors during request processing in the
     * LoggingFilter class. It extends the ResponseLoggingErrorInterceptor class.
     *
     * @param addIdHeader Boolean indicating whether to add an ID header to the response.
     * @param delegateRs The delegate ServerResponse object.
     * @param rawUtils The Utils object used for logging functionality.
     * @param startTimeAttribute The attribute name for the start time of the request/response.
     */
    open class ErrorInterceptor(
        addIdHeader: Boolean,
        delegateRs: ServerResponse,
        protected val rawUtils: Utils,
        startTimeAttribute: String = "startTime",
    ) : SpringCloudGatewayLoggingErrorWebExceptionHandler.ResponseLoggingErrorInterceptor(addIdHeader, delegateRs, rawUtils, startTimeAttribute) {

        override fun writeTo(
            exchange: ServerWebExchange,
            context: ServerResponse.Context,
        ): Mono<Void> {
            val proxyConfig = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD] as ProxyRouteConfigDTO?
            val logBody = proxyConfig?.logBody ?: true
            val logHeaders = proxyConfig?.logHeaders ?: true
            val withLog =
                exchange.mutate().response(
                    ResponseInterceptor(
                        logBody, logHeaders,
                        addIdHeader,
                        exchange.response,
                        exchange.request,
                        exchange.attributes[startTimeAttribute] as Long?,
                        rawUtils,
                    ),
                ).build()
            return delegateRs.writeTo(withLog, context)
        }
    }
}

fun HttpHeaders.asString(): String = this.asSequence().map { "${it.key}:${it.value.joinToString(",")}" }.joinToString(";")


