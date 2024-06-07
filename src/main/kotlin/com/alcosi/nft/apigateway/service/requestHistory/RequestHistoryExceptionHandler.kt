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

package com.alcosi.nft.apigateway.service.requestHistory

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import io.github.breninsul.namedlimitedvirtualthreadexecutor.service.VirtualTreadExecutor
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

/**
 * This class is responsible for handling exceptions and generating
 * response with request history information.
 *
 * @param dBService The service used to access the request history
 *     database.
 * @param requestHistoryInfoFiled The field name used to store request
 *     history information in the attributes of the exchange.
 * @param addIdHeader Indicates whether to add an ID header to the
 *     response.
 * @param utils The utility class for Spring Cloud Gateway logging.
 * @param errorAttributes The error attributes to be used for rendering
 *     error responses.
 * @param resources The web resources configuration.
 * @param errorProperties The error properties configuration.
 * @param applicationContext The application context of the Spring
 *     application.
 */
open class RequestHistoryExceptionHandler(
    protected val dBService: RequestHistoryDBService,
    protected val requestHistoryInfoFiled: String = PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO,
    addIdHeader: Boolean,
    utils: SpringCloudGatewayLoggingUtils,
    errorAttributes: ErrorAttributes,
    resources: WebProperties.Resources,
    errorProperties: ErrorProperties,
    applicationContext: ApplicationContext,
) : SpringCloudGatewayLoggingErrorWebExceptionHandler(addIdHeader, utils, errorAttributes, resources, errorProperties, applicationContext) {
    /**
     * Renders an error response for the given ServerRequest.
     *
     * @param request The ServerRequest object representing the request.
     * @return A Mono that emits a ServerResponse object.
     */
    override fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        val rsMono = super.renderErrorResponse(request)
        return rsMono.map { RequestHistoryResponse(dBService, requestHistoryInfoFiled, it) }
    }

    /**
     * Represents a server response used for request history.
     *
     * @property dBService The RequestHistoryDBService used for managing
     *     request history in a database.
     * @property requestHistoryInfoFiled The field name for storing request
     *     history info in the exchange attributes. Default is
     *     PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY _INFO.
     * @property delegateRs The delegated ServerResponse object.
     */
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

    /**
     * RequestHistoryResponseInterceptor intercepts the response from the
     * server and saves the response information to the database.
     *
     * @property info The request history information.
     * @property dBService The RequestHistoryDBService instance for interacting
     *     with the database.
     * @property delegateRs The delegate ServerHttpResponse object.
     */
    open class RequestHistoryResponseInterceptor(
        protected val info: RequestHistoryDBService.HistoryRqInfo,
        protected val dBService: RequestHistoryDBService,
        protected val delegateRs: ServerHttpResponse,
    ) : ServerHttpResponseDecorator(delegateRs) {
        /**
         * Overrides the `writeWith` method of the base class
         * [ServerHttpResponseDecorator] to intercept the response from the server,
         * save the response information to the database, and return a [Mono]
         * representing the completion of the write operation.
         *
         * @param body The [Publisher] providing the response body [DataBuffer]s.
         * @return A [Mono] representing the completion of the write operation.
         */
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            val deferredContentLength = AtomicLong(0)
            val countedBody =
                Flux.from(body).map { dataBuffer ->
                    deferredContentLength.addAndGet(dataBuffer.readableByteCount().toLong())
                    dataBuffer
                }
            return super.writeWith(countedBody).doOnSuccess { executor.execute { dBService.saveRs(info, deferredContentLength.get(), statusCode) } }
        }
    }

    /**
     * The Companion class represents the companion object of a class. In
     * Kotlin, the companion object is declared inside the class and can
     * contain properties and methods that are related to the class itself.
     *
     * @property executor The executor used by the class. It is protected and
     *     can be accessed by subclasses.
     */
    companion object {
        /**
         * An open protected property representing the executor for running tasks.
         * By default, it creates a new `VirtualThreadPerTaskExecutor`.
         */
        protected open val executor = VirtualTreadExecutor
    }
}
