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
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.LocalDateTime
import java.util.UUID

open class RequestHistoryGatewayFilterSecurity(
    protected val requestHistoryDBComponent: RequestHistoryDBComponent,
    val securityClientAttributeField: String = SecurityGatewayFilter.SECURITY_CLIENT_ATTRIBUTE,
    protected val rqTimeFiled:String =PathConfigurationComponent.ATTRIBUTES_REQUEST_TIME,
    protected val requestHistoryIdMonoFiled:String =PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_ID_MONO,
    private val order: Int = SecurityGatewayFilter.SECURITY_LOG_ORDER
) : MicroserviceGatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val client = exchange.attributes[securityClientAttributeField]
        if (client!=null&&client is PrincipalDetails){
            val historyIdMono=exchange.attributes[requestHistoryIdMonoFiled] as Mono<Long>
            val rqTime=exchange.attributes[rqTimeFiled] as LocalDateTime
            val userId=if (client is UserDetails) UUID.fromString(client.id) else null
            val accountId=if (client is ClientAccountDetails) UUID.fromString(client.id) else null
            val updatedHistoryIdMono=requestHistoryDBComponent.saveAuth(historyIdMono,rqTime,userId,accountId,client).cache()
            updatedHistoryIdMono.subscribe()
        }
        return chain.filter(exchange)

    }
    override fun getOrder(): Int {
        return order
    }

}