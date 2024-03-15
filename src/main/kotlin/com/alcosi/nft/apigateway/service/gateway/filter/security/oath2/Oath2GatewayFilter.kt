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
