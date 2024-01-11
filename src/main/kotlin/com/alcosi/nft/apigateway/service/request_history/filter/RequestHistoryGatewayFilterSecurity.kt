package com.alcosi.nft.apigateway.service.request_history.filter

import com.alcosi.lib.security.PrincipalDetails
import com.alcosi.lib.security.UserDetails
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import com.alcosi.nft.apigateway.config.path.dto.SecurityRouteConfigDTO
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.ClientAccountDetails
import com.alcosi.nft.apigateway.service.request_history.RequestHistoryDBComponent
import com.alcosi.nft.apigateway.service.request_history.RequestHistoryDBService
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.LocalDateTime
import java.util.UUID

open class RequestHistoryGatewayFilterSecurity(
    protected val requestHistoryDBService: RequestHistoryDBService,
    private val order: Int = SecurityGatewayFilter.SECURITY_LOG_ORDER
) : MicroserviceGatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        requestHistoryDBService.saveAuth(exchange)
        return chain.filter(exchange)

    }
    override fun getOrder(): Int {
        return order
    }

}