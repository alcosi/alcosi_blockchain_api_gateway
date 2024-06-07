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
import com.alcosi.nft.apigateway.config.path.dto.SecurityRouteConfigDTO
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

/**
 * SecurityConfigGatewayPredicate is a class that represents the security configuration for a gateway predicate.
 *
 * @param delegate The filter match predicate used for request filtering.
 * @param securityConfig The security route configuration.
 */
open class SecurityConfigGatewayPredicate(
    open val delegate: FilterMatchPredicate,
    open val securityConfig: SecurityRouteConfigDTO,
) : Logging, Predicate<ServerWebExchange> {
    /**
     * Determines whether the ServerWebExchange matches the criteria to pass the test.
     *
     * @param t The ServerWebExchange to be tested.
     * @return true if the ServerWebExchange matches the criteria, false otherwise.
     */
    override fun test(t: ServerWebExchange): Boolean {
        val haveToPass = delegate.test(t)
        setConfig(haveToPass, t)
        return haveToPass
    }

    /**
     * Sets the configuration attribute for the given ServerWebExchange.
     *
     * @param haveToPass Determines whether the configuration should be set.
     * @param t The ServerWebExchange to set the configuration attribute on.
     */
    protected open fun setConfig(
        haveToPass: Boolean,
        t: ServerWebExchange,
    ) {
        if (haveToPass) {
            t.attributes[PathConfigurationComponent.ATTRIBUTE_SECURITY_CONFIG_FIELD] = securityConfig
        }
    }
}
