package com.alcosi.nft.apigateway.service.requestHistory

import com.alcosi.lib.security.ClientAccountDetails
import com.alcosi.lib.security.PrincipalDetails
import com.alcosi.lib.security.UserDetails
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.PathAuthorities
import com.alcosi.nft.apigateway.config.path.dto.PathAuthority
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import com.alcosi.nft.apigateway.config.path.dto.SecurityRouteConfigDTO
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

open class RequestHistoryDBService(
    protected val component: RequestHistoryDBComponent,
    protected val ipHeader: String = "x-real-ip",
) {
    data class RouteDetails(
        val proxyConfig: ProxyRouteConfigDTO?,
        val securityConfig: SecurityRouteConfigDTO?,
        val requiredAuthorities: PathAuthorities?,
    )

    data class HistoryRqInfo(
        val idMono: Mono<Long>,
        val rqTime: LocalDateTime,
    )

    data class HistoryRsInfo(
        val idMono: Mono<Long>,
        val rqTime: LocalDateTime,
        val rsTime: LocalDateTime,
    )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun saveRequest(exchange: ServerWebExchange): HistoryRqInfo {
        val request = exchange.request
        val rqHeaders = request.headers
        val routeDetails =
            RouteDetails(
                exchange.attributes[PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD] as ProxyRouteConfigDTO?,
                exchange.attributes[PathConfigurationComponent.ATTRIBUTE_SECURITY_CONFIG_FIELD] as SecurityRouteConfigDTO?,
                exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] as PathAuthorities?,
            )
        val rqSize = (rqHeaders[HttpHeaders.CONTENT_LENGTH] ?: rqHeaders["${HttpHeaders.CONTENT_LENGTH}_ORIGINAL"])?.first()?.toLongOrNull() ?: rqHeaders.contentLength
        val rqTime = LocalDateTime.now()
        exchange.attributes[ PathConfigurationComponent.ATTRIBUTES_REQUEST_TIME] = rqTime
        val historyIdMono =
            component.saveRequest(
                request.id,
                rqHeaders,
                request.getIp(),
                request.getUri(),
                request.method,
                rqSize,
                rqTime,
                routeDetails.proxyConfig?.name ?: routeDetails.proxyConfig?.microserviceUri ?: "NONE",
                routeDetails,
            )
                .cache()
        historyIdMono.subscribe()
        val historyRqInfo = HistoryRqInfo(historyIdMono, rqTime)
        exchange.attributes[PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO] = historyRqInfo
        return historyRqInfo
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun saveAuth(exchange: ServerWebExchange): HistoryRqInfo? {
        val client = exchange.attributes[SecurityGatewayFilter.SECURITY_CLIENT_ATTRIBUTE]
        val requestHistoryInfo = exchange.attributes[PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO] as HistoryRqInfo?
        if (client != null && client is PrincipalDetails) {
            val userId = if (client is UserDetails) UUID.fromString(client.id) else null
            val accountId = if (client is ClientAccountDetails) UUID.fromString(client.id) else null
            val updatedHistoryIdMono = component.saveAuth(requestHistoryInfo!!.idMono, requestHistoryInfo.rqTime, userId, accountId, client).cache()
            updatedHistoryIdMono.subscribe()
            return HistoryRqInfo(updatedHistoryIdMono, requestHistoryInfo.rqTime)
        }
        return requestHistoryInfo
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun saveRs(
        info: HistoryRqInfo,
        rsSize: Long?,
        rsStatusCode: HttpStatusCode?,
    ): HistoryRsInfo {
        val rsTime = LocalDateTime.now()
        val idMono = component.saveRs(info.idMono, info.rqTime, rsTime, rsSize, rsStatusCode?.value()).cache()
        idMono.subscribe()
        return HistoryRsInfo(idMono, info.rqTime, rsTime)
    }

    open fun ServerHttpRequest.getIp() = headers[ipHeader]?.firstOrNull() ?: remoteAddress?.hostString!!

    open fun ServerHttpRequest.getUri(): String {
        val params =
            queryParams
                .entries.joinToString("&") { "${it.key}:${it.value.joinToString(",")}" }
        return path.toString() + (if (params.isBlank()) params else "?$params")
    }
}
