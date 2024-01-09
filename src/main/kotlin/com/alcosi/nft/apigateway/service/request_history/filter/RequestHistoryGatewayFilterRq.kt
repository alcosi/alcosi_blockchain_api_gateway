package com.alcosi.nft.apigateway.service.request_history.filter

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import com.alcosi.nft.apigateway.config.path.dto.SecurityRouteConfigDTO
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.request_history.RequestHistoryDBComponent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.Logging
import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime

open class RequestHistoryGatewayFilterRq(
    protected val requestHistoryDBComponent: RequestHistoryDBComponent,
    protected val ipHeader: String = "x-real-ip",
    protected val proxyConfigField: String = PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD,
    protected val attrSecurityConfigFiled: String = PathConfigurationComponent.ATTRIBUTE_SECURITY_CONFIG_FIELD,
    protected val authoritiesAttributeField: String = PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD,
    protected val rqTimeFiled: String = PathConfigurationComponent.ATTRIBUTES_REQUEST_TIME,
    protected val requestHistoryIdMonoFiled: String = PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_ID_MONO,
    private val order: Int = Int.MIN_VALUE
) : MicroserviceGatewayFilter {
    data class RouteDetails(
        val proxyConfig: ProxyRouteConfigDTO?,
        val securityConfig: SecurityRouteConfigDTO?,
        val requiredAuthorities: List<String>?
    )

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val rqHeaders = request.headers
        val routeDetails = RouteDetails(
            exchange.attributes[proxyConfigField] as ProxyRouteConfigDTO?,
            exchange.attributes[attrSecurityConfigFiled] as SecurityRouteConfigDTO?,
            exchange.attributes[authoritiesAttributeField] as List<String>?
        )
        val rqTime = LocalDateTime.now()
        exchange.attributes[rqTimeFiled] = rqTime
        val historyIdMono = requestHistoryDBComponent.saveRequest(
            request.id,
            rqHeaders,
            request.getIp(),
            request.getUri(),
            request.method,
            request.headers.contentLength,
            rqTime,
            routeDetails.proxyConfig?.name ?: routeDetails.proxyConfig?.microserviceUri ?: "NONE",
            routeDetails
        )
            .cache()
        exchange.attributes[requestHistoryIdMonoFiled] = historyIdMono
        historyIdMono.subscribe()
        val serverWebExchange = exchange.mutate()
            .response(RequestHistoryResponseDecorator(exchange, requestHistoryDBComponent, rqTime, historyIdMono))
            .build()
        return chain.filter(serverWebExchange).then(
            Mono.just(serverWebExchange.response).flatMap {
                logger.log(Level.WARN,"Response code ${it.statusCode}")
                return@flatMap Mono.just("").then()
            }

        )
    }

    open fun ServerHttpRequest.getIp(
    ) = headers[ipHeader]?.firstOrNull() ?: remoteAddress?.hostString!!

    open fun ServerHttpRequest.getUri(): String {
        val params = queryParams
            .entries.joinToString("&") { "${it.key}:${it.value.joinToString(",")}" }
        return path.toString() + (if (params.isBlank()) params else "?$params")
    }

    override fun getOrder(): Int {
        return order
    }

    open class RequestHistoryResponseDecorator(
        protected val exchange: ServerWebExchange,
        protected val requestHistoryDBComponent: RequestHistoryDBComponent,
        protected val rqTime: LocalDateTime,
        protected val historyIdMono: Mono<Long>,
    ) : ServerHttpResponseDecorator(exchange.response), Logging {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            return super
                .writeWith(body)
                .then(requestHistoryDBComponent.saveRs(historyIdMono, rqTime, LocalDateTime.now(),  headers.contentLength, statusCode.value()).then())
        }
    }
}