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

package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2
import com.alcosi.lib.filters.servlet.HeaderHelper.Companion.ACCOUNT_DETAILS
import com.alcosi.lib.filters.servlet.HeaderHelper.Companion.ACCOUNT_ID
import com.alcosi.lib.filters.servlet.HeaderHelper.Companion.ORIGINAL_AUTHORISATION
import com.alcosi.lib.filters.servlet.HeaderHelper.Companion.USER_DETAILS
import com.alcosi.lib.filters.servlet.HeaderHelper.Companion.USER_ID
import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.lib.secured.encrypt.SensitiveComponent
import com.alcosi.lib.security.AccountDetails
import com.alcosi.lib.security.UserDetails
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

open class Oath2GatewayFilter(
    securityGatewayFilter: SecurityGatewayFilter,
    protected open val getUserInfoService: Oath2UserInfoProvider,
    protected open val mappingHelper: MappingHelper,
    authHeaders: List<String> = listOf(USER_ID, ACCOUNT_ID, USER_ID, USER_DETAILS, ACCOUNT_DETAILS, ORIGINAL_AUTHORISATION),
    val sensitiveComponent: SensitiveComponent,
    order: Int = JWT_LOG_ORDER,
) : JwtGatewayFilter(securityGatewayFilter, authHeaders, order) {
    override fun mutateExchange(
        token: String,
        exchange: ServerWebExchange,
        clientAttribute: String,
    ): Mono<ServerWebExchange> {
        return getUserInfoService.getInfo(token)
            .map { account ->
                val rqBuilder =
                    exchange.request
                        .mutate()
                rqBuilder.header(ORIGINAL_AUTHORISATION, sensitiveComponent.serialize( token.toByteArray()))
                when (account) {
                    is AccountDetails -> {
                        rqBuilder
                            .header(ACCOUNT_ID, account.id)
                            .header(ACCOUNT_DETAILS, mappingHelper.serialize(account))
                    }
                    is UserDetails -> {
                        rqBuilder
                            .header(USER_ID, account.id)
                            .header(USER_DETAILS, mappingHelper.serialize(account))
                    }
                    else -> throw IllegalStateException("Wrong account type class ${account.javaClass}")
                }
                exchange.attributes[clientAttribute] = account
                exchange.mutate().request(rqBuilder.build()).build()
            }
    }
}
