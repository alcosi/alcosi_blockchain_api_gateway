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

abstract class LoginAbstractGatewayFilter(
    val basePath: String,
    val writer: GatewayFilterResponseWriter,
    val methods: List<HttpMethod>,
    protected val uriRegexString: String,
    val loginProcessors: List<LoginRequestProcess>,
    val prepareHexService: PrepareHexService,
    private val order: Int = JWT_LOG_ORDER + 30,
) : ControllerGatewayFilter {
    protected val uriPattern: Pattern = uriRegexString.replace("/", "\\/").toPattern()
    protected val beforeFilters = loginProcessors.filter { it.types().contains(LoginRequestProcess.TYPE.BEFORE) }
    protected val afterFilters = loginProcessors.filter { it.types().contains(LoginRequestProcess.TYPE.AFTER) }

    override fun getOrder(): Int {
        return order
    }

    override fun matches(request: ServerHttpRequest): Boolean {
        val toString = request.path.toString()
        val matches = uriPattern.matcher(toString).matches()
        return matches && (methods.contains(request.method))
    }

    protected fun getWallet(req: ServerHttpRequest): String {
        val matcher = uriPattern.matcher(basePath + req.path.toString())
        if (!matcher.matches()) {
            throw IllegalStateException("Mot matches uri ${req.path}/$uriPattern")
        }
        return matcher.group("wallet")
    }

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

    abstract fun internal(
        wallet: String,
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Any>
}
