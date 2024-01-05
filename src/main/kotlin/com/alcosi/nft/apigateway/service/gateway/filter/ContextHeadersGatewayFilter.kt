package com.alcosi.nft.apigateway.service.gateway.filter

import com.alcosi.lib.filters.HeaderHelper
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

open class ContextHeadersGatewayFilter(
    val serviceName: String,
    val environment: String,
    private val order: Int = Int.MIN_VALUE
) : MicroserviceGatewayFilter {
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val apiKey = (exchange.attributes[PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD] as ProxyRouteConfigDTO?)?.apiKey
        val builder = exchange
            .request
            .mutate()
            .header(HeaderHelper.ENV_HEADER, environment)
            .header(HeaderHelper.SERVICE_NAME, serviceName)
        if (apiKey!=null){
            builder.header(HeaderHelper.SERVICE_AUTH_HEADER,"Bearer $apiKey")
        }
        val decorated = exchange.mutate().request(builder.build()).build()
        return chain.filter(decorated)

    }

    override fun getOrder(): Int {
        return order
    }
}