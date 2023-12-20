package com.alcosi.nft.apigateway.service.requestHistory.filter

import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.requestHistory.RequestHistoryDBService
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

open class RequestHistoryGatewayFilterSecurity(
    protected val requestHistoryDBService: RequestHistoryDBService,
    private val order: Int = SecurityGatewayFilter.SECURITY_LOG_ORDER,
) : MicroserviceGatewayFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        requestHistoryDBService.saveAuth(exchange)
        return chain.filter(exchange)
    }

    override fun getOrder(): Int {
        return order
    }
}
