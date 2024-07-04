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
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent.Companion.ATTRIBUTES_REQUEST_HISTORY_INFO
import com.alcosi.nft.apigateway.config.path.dto.PathAuthorities
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import com.alcosi.nft.apigateway.config.path.dto.SecurityRouteConfigDTO
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import org.apache.logging.log4j.kotlin.logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * This class represents a service for managing request history in a database.
 *
 * @param component The database component responsible for saving request history data.
 * @param ipHeader The name of the header containing the client's IP address. Default is "x-real-ip".
 * @param maskHeaders The list of headers that should be masked when saving the request history.
 */
open class RequestHistoryDBService(
    protected val component: RequestHistoryDBComponent,
    protected val ipHeader: String = "x-real-ip",
    protected val maskHeaders: List<String>
) {
    /**
     * Represents the details of a route.
     *
     * @property proxyConfig The proxy route configuration.
     * @property securityConfig The security route configuration.
     * @property requiredAuthorities The required authorities for the route.
     */
    data class RouteDetails(
        val proxyConfig: ProxyRouteConfigDTO?,
        val securityConfig: SecurityRouteConfigDTO?,
        val requiredAuthorities: PathAuthorities?,
    )

    /**
     * HistoryRqInfo is a data class that represents the information related to a request history.
     *
     * @property idMono The Mono that represents the ID of the request history.
     * @property rqTime The LocalDateTime object that represents the time of the request.
     */
    data class HistoryRqInfo(
        val idMono: Mono<Long>,
        val rqTime: LocalDateTime,
    )

    /**
     * Represents the information related to the response of a history record.
     *
     * @property idMono A [Mono] emitting the ID of the history record.
     * @property rqTime The [LocalDateTime] representing the request time.
     * @property rsTime The [LocalDateTime] representing the response time.
     */
    data class HistoryRsInfo(
        val idMono: Mono<Long>,
        val rqTime: LocalDateTime,
        val rsTime: LocalDateTime,
    )

    /**
     * Saves the request information to the database and returns the associated history ID.
     *
     * This method is annotated with `@Transactional` to ensure that it is executed within a new transaction.
     *
     * @param exchange The ServerWebExchange object representing the request and response.
     * @return The HistoryRqInfo object containing the ID of the request history and the time of the request.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun saveRequest(exchange: ServerWebExchange,historyRqInfoCompletableFuture:CompletableFuture<HistoryRqInfo>): HistoryRqInfo {
        try {

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
            historyRqInfoCompletableFuture.complete(historyRqInfo)
            return historyRqInfo
        }catch (t:Throwable){
            historyRqInfoCompletableFuture.completeExceptionally(t)
            throw t
        }
    }

    /**
     * Saves authentication information to the database and returns the associated history info.
     *
     * This method is annotated with @Transactional(propagation = Propagation.REQUIRES_NEW)
     * to ensure that it is executed within a new transaction.
     *
     * @param exchange The ServerWebExchange object representing the request and response.
     * @return The HistoryRqInfo object containing the ID of the request history and the time of the request.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun saveAuth(exchange: ServerWebExchange): HistoryRqInfo? {
        val client = exchange.attributes[SecurityGatewayFilter.SECURITY_CLIENT_ATTRIBUTE]
        val time=System.currentTimeMillis()
        val requestHistoryInfoFuture = exchange.attributes[ATTRIBUTES_REQUEST_HISTORY_INFO]!! as CompletableFuture<RequestHistoryDBService.HistoryRqInfo>

        val requestHistoryInfo = requestHistoryInfoFuture.get()
        val took=System.currentTimeMillis()-time
        if (took>1) {
            logger.info("DB RequestHistoryDBService waiting took $took ms")
        }
        if (client != null && client is PrincipalDetails) {
            val userId = if (client is UserDetails) UUID.fromString(client.id) else null
            val accountId = if (client is AccountDetails) UUID.fromString(client.id) else null
            val updatedHistoryIdMono = component.saveAuth(requestHistoryInfo!!.idMono, requestHistoryInfo.rqTime, userId, accountId, client).cache()
            updatedHistoryIdMono.subscribe()
            return HistoryRqInfo(updatedHistoryIdMono, requestHistoryInfo.rqTime)
        }
        return requestHistoryInfo
    }

    /**
     * Saves the response information to the database and returns the associated response history.
     *
     * @param info The HistoryRqInfo object representing the request history information.
     * @param rsSize The size of the response in bytes. Can be null if unknown.
     * @param rsStatusCode The HTTP status code of the response. Can be null if unknown.
     * @return The HistoryRsInfo object containing the ID of the response history, the time of the request, and the time of the response.
     */
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

    /**
     * Retrieves the IP address from the ServerHttpRequest object.
     *
     * If the IP address is present in the headers, the first occurrence is returned.
     * If the IP address is not found in the headers, the hostString of the remoteAddress is returned.
     *
     * @return The IP address as a String.
     */
    open fun ServerHttpRequest.getIp() = headers[ipHeader]?.firstOrNull() ?: remoteAddress?.hostString!!

    /**
     * Retrieves the URI of the request including the path and query parameters.
     * The query parameters are appended to the path in the format "key:value1,value2&key2:value3,value4".
     *
     * @return The URI as a String.
     */
    open fun ServerHttpRequest.getUri(): String {
        val params =
            queryParams
                .entries.joinToString("&") { "${it.key}:${it.value.joinToString(",")}" }
        return path.toString() + (if (params.isBlank()) params else "?$params")
    }
}
