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

package com.alcosi.nft.apigateway.service.predicate

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.ProxyRouteConfigDTO
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

/**
 * Represents a gateway predicate for route configuration.
 *
 * @property delegate The delegate match predicate used for filtering requests.
 * @property proxyConfig The proxy route configuration.
 */
open class RouteConfigGatewayPredicate(
    open val delegate: FilterMatchPredicate,
    open val proxyConfig: ProxyRouteConfigDTO,
) : Logging, Predicate<ServerWebExchange> {
    /**
     * Overrides the `test` method of the `RouteConfigGatewayPredicate` class.
     * Tests the `ServerWebExchange` against the configured criteria to determine if it matches.
     *
     * @param t The `ServerWebExchange` to be tested.
     * @return True if the `ServerWebExchange` matches the criteria, false otherwise.
     */
    override fun test(t: ServerWebExchange): Boolean {
        val haveToPass = delegate.test(t)
        setConfig(haveToPass, t)
        return haveToPass
    }

    /**
     * Sets the config attribute in the ServerWebExchange if haveToPass is true.
     *
     * @param haveToPass Specifies whether the config attribute should be set.
     * @param t The ServerWebExchange object to set the config attribute on.
     */
    protected open fun setConfig(
        haveToPass: Boolean,
        t: ServerWebExchange,
    ) {
        if (haveToPass) {
            t.attributes[PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD] = proxyConfig
        }
    }
}
