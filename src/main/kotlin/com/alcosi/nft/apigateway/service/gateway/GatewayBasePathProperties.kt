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
package com.alcosi.nft.apigateway.service.gateway

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The GatewayBasePathProperties class represents the configuration properties for the base path of a gateway.
 *
 * @property path The base path for the gateway.
 * @property fakeUri The fake URI to be used for testing purposes.
 */
@ConfigurationProperties("gateway.base")
open class GatewayBasePathProperties {
    /**
     * Represents the base path for the gateway.
     *
     * @property path The base path for the gateway.
     */
    var path: String = "/api"

    /**
     * Represents the fake URI to be used for testing purposes.
     */
    var fakeUri: String = "http://127.0.200.1:87787"
}
