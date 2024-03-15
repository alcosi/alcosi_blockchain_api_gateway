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

import com.alcosi.lib.security.AccountDetails
import com.alcosi.lib.security.PrincipalDetails
import com.alcosi.lib.security.UserDetails
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.PathAuthorities
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
    protected val maskHeaders: List<String>
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
        val rqHeadersMap = request.headers
            .toMap()
            .mapValues { it.value?.joinToString("\n") }
            .mapValues { if (maskHeaders.any { mh -> mh.equals(it.key, true) }) it.value?.let { _ -> "<masked>" } else it.value }
        val securityConfig = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_SECURITY_CONFIG_FIELD] as SecurityRouteConfigDTO?
        val proxyConfig = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD] as ProxyRouteConfigDTO?
        val requiredAuthorities = exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] as PathAuthorities?
        val routeDetails =
            RouteDetails(
                proxyConfig?.copy(apiKey = proxyConfig.apiKey?.let { "<masked>" }),
                securityConfig,
                requiredAuthorities,
            )
        val rqSize = (rqHeaders[HttpHeaders.CONTENT_LENGTH] ?: rqHeaders["${HttpHeaders.CONTENT_LENGTH}_ORIGINAL"])?.first()?.toLongOrNull() ?: rqHeaders.contentLength
        val rqTime = LocalDateTime.now()
        exchange.attributes[PathConfigurationComponent.ATTRIBUTES_REQUEST_TIME] = rqTime
        val historyIdMono =
            component.saveRequest(
                request.id,
                rqHeadersMap,
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
            val accountId = if (client is AccountDetails) UUID.fromString(client.id) else null
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
