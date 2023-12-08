/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.alcosi.nft.apigateway.service.gateway.filter.login

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.service.LoginRequestProcess
import com.alcosi.nft.apigateway.service.gateway.filter.ControllerGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.GatewayFilterResponseWriter
import com.alcosi.nft.apigateway.service.gateway.filter.security.JWT_LOG_ORDER
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
    ) : ControllerGatewayFilter {
    protected val uriPattern: Pattern = uriRegexString.replace("/", "\\/").toPattern()
    protected val beforeFilters = loginProcessors.filter { it.types().contains(LoginRequestProcess.TYPE.BEFORE) }
    protected val afterFilters = loginProcessors.filter { it.types().contains(LoginRequestProcess.TYPE.AFTER) }

    override fun getOrder(): Int {
        return JWT_LOG_ORDER + 30;
    }

    override fun matches(request: ServerHttpRequest): Boolean {
        val toString = request.path.toString()
        val matches = uriPattern.matcher(toString).matches()
        return matches &&(methods.contains(request.method))
    }


    protected fun getWallet(req: ServerHttpRequest): String {
        val matcher = uriPattern.matcher(basePath + req.path.toString())
        if (!matcher.matches()) {
            throw IllegalStateException("Mot matches uri ${req.path}/${uriPattern}")
        }
        return matcher.group("wallet")
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val wallet = prepareHexService.prepareAddr(getWallet(exchange.request))
        val processedBefore = beforeFilters.fold(Mono.empty<Void>()) { a, b -> a.then(b.process(wallet)) }
        val result= processedBefore.then(internal(wallet, exchange, chain)).cache()
        val processed=afterFilters.fold(result) { a, b ->
            a.flatMap { b.process(wallet).then(Mono.just(it)) }
        }
        return writer.writeMonoJson(exchange.response, processed)
    }

    abstract fun internal(wallet:String,exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Any>
}