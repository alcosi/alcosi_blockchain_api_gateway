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

/**
 * Represents the order value for a Controller gateway filter in a Spring Cloud Gateway.
 *
 * The CONTROLLER_ORDER constant defines the highest possible order value, which ensures that the Controller gateway filters
 * are executed last in the filter chain.
 *
 * It is typically used in the `getOrder()` method of a ControllerGatewayFilter implementation, to specify the execution order
 * of the filter.
 */
const val CONTROLLER_ORDER = Int.MAX_VALUE

/**
 * Represents a Gateway filter for controlling requests to a Controller.
 */
interface ControllerGatewayFilter : GatewayFilter, Ordered, Logging {
    /**
     * Determines if the given `request` matches the criteria for the gateway filter.
     *
     * @param request the server HTTP request to be evaluated
     * @return `true` if the `request` matches the criteria, `false` otherwise
     */
    fun matches(request: ServerHttpRequest): Boolean

    /**
     * Gets the order value of this ControllerGatewayFilter.
     *
     * This method returns the order value for controlling the execution order of ControllerGatewayFilters.
     * The order value is used by the GatewayFilterChain to determine the sequence in which filters are applied
     * to incoming requests.
     *
     * @return the order value of this ControllerGatewayFilter
     */
    override fun getOrder(): Int {
        return CONTROLLER_ORDER
    }
}
