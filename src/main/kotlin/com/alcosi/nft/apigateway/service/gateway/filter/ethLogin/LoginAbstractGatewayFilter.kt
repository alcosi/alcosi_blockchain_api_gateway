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

package com.alcosi.nft.apigateway.service.gateway.filter.ethLogin

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.LoginRequestProcess
import com.alcosi.nft.apigateway.service.gateway.filter.ControllerGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter.Companion.JWT_LOG_ORDER
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.regex.Pattern

/**
 * An abstract class for implementing a login gateway filter.
 *
 * @property basePath The base path of the filter.
 * @property writer The response writer for the filter.
 * @property methods The allowed HTTP methods for the filter.
 * @property uriRegexString The regular expression pattern for matching the URI.
 * @property loginProcessors The list of login request processors.
 * @property prepareHexService The service for preparing hex values.
 * @property order The order of the filter.
 */
abstract class LoginAbstractGatewayFilter(
    val basePath: String,
    val writer: GatewayFilterResponseWriter,
    val methods: List<HttpMethod>,
    protected val uriRegexString: String,
    val loginProcessors: List<LoginRequestProcess>,
    val prepareHexService: PrepareHexService,
    private val order: Int = JWT_LOG_ORDER + 30,
) : ControllerGatewayFilter {
    /**
     * The regular expression pattern used for matching URIs.
     */
    protected val uriPattern: Pattern = uriRegexString.replace("/", "\\/").toPattern()

    /**
     * Represents a collection of filters to be executed before processing a request.
     * The filters are extracted from the loginProcessors collection, filtering only those with a "BEFORE" type.
     *
     * @property beforeFilters a list of filters to be executed before processing a request.
     */
    protected val beforeFilters = loginProcessors.filter { it.types().contains(LoginRequestProcess.TYPE.BEFORE) }

    /**
     * Contains the list of after filters for login processors.
     *
     * The afterFilters property is a protected property that holds the list of login processors
     * retrieved from the loginProcessors property. It filters the processors based on the types
     * returned by the `types()` function of each processor.
     *
     * @property afterFilters a list of login processors filtered based on the types returned
     * by the `types()` function
     */
    protected val afterFilters = loginProcessors.filter { it.types().contains(LoginRequestProcess.TYPE.AFTER) }

    /**
     * Returns the order of the LoginAbstractGatewayFilter.
     *
     * @return the order of the LoginAbstractGatewayFilter
     */
    override fun getOrder(): Int {
        return order
    }

    /**
     * Determines if the given request matches the specified criteria.
     *
     * @param request the ServerHttpRequest to be matched
     * @return true if the request matches the criteria, false otherwise
     */
    override fun matches(request: ServerHttpRequest): Boolean {
        val toString = request.path.toString()
        val matches = uriPattern.matcher(toString).matches()
        return matches && (methods.contains(request.method))
    }

    /**
     * Retrieves the wallet from the provided ServerHttpRequest.
     *
     **/
    protected fun getWallet(req: ServerHttpRequest): String {
        val matcher = uriPattern.matcher(basePath + req.path.toString())
        if (!matcher.matches()) {
            throw IllegalStateException("Mot matches uri ${req.path}/$uriPattern")
        }
        return matcher.group("wallet")
    }

    /**
     * Filters the incoming request based on specified criteria.
     *
     * @param exchange the ServerWebExchange containing the request and response
     * @param chain the GatewayFilterChain for executing the next filters
     * @return a Mono representing the completion of the filter operation
     */
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val wallet = prepareHexService.prepareAddr(getWallet(exchange.request))
        val processedBefore = beforeFilters.fold(Mono.empty<Void>()) { a, b -> a.then(b.process(wallet)) }
        val result = processedBefore.then(internal(wallet, exchange, chain)).cache()
        val processed =
            afterFilters.fold(result) { a, b ->
                a.flatMap { b.process(wallet).then(Mono.just(it)) }
            }
        return writer.writeMonoJson(exchange.response, processed)
    }

    /**
     * Performs an internal operation with the given wallet, exchange, and chain.
     *
     * @param wallet the wallet to be used
     * @param exchange the ServerWebExchange containing the request and response
     * @param chain the GatewayFilterChain for executing the next filters
     * @return a Mono representing the completion of the operation
     */
    abstract fun internal(
        wallet: String,
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Any>
}
