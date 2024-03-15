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

package com.alcosi.nft.apigateway.service.gateway.filter

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpRequest

const val CONTROLLER_ORDER = Int.MAX_VALUE

interface ControllerGatewayFilter : GatewayFilter, Ordered, Logging {
    fun matches(request: ServerHttpRequest): Boolean

    override fun getOrder(): Int {
        return CONTROLLER_ORDER
    }
}
