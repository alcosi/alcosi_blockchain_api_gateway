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
package com.alcosi.nft.apigateway.auth.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpMethod

/**
 * Configuration properties for the EthJwtLoginProcessor.
 */
@ConfigurationProperties("gateway.default-request-login-request-process")
open class EthJwtLoginProcessorProperties {
    /**
     * The `enabled` variable indicates whether a certain feature or functionality is enabled or not.
     *
     * @property enabled a boolean value representing the enabled state. `true` indicates that the feature is enabled, while `false` indicates that it is disabled.
     */
    var enabled: Boolean = true

    /**
     * The serviceUri variable represents the URI of a service.
     */
    var serviceUri: String? = null

    /**
     * Represents the HTTP method used in the service request.
     *
     * The `serviceMethod` variable can be set to one of the following values:
     * - GET: indicates a GET request method.
     * - POST: indicates a POST request method.
     * - PUT: indicates a PUT request method.
     *
     * If the `serviceMethod` is not set, it defaults to `null`.
     *
     * This variable is used in the `DefaultRequestLoginRequestProcess` class to determine the HTTP method used for the login request process.
     * It is also used in the `LoginRequestProcess` interface to define the available request types.
     *
     * @see DefaultRequestLoginRequestProcess
     * @see LoginRequestProcess
     * @see HttpMethod
     */
    var serviceMethod: HttpMethod? = null

    /**
     * Represents a list of different types of login request processes.
     *
     * This variable is used to specify the types of login request processes available. The type can be one of the following:
     * - RequestType.GET: Indicates a GET request method.
     * - RequestType.POST: Indicates a POST request method.
     * - RequestType.PUT: Indicates a PUT request method.
     *
     * The rqTypes variable is used within the EthJwtLoginProcessorProperties class to configure the supported request types for login request processes. The rqTypes property allows
     *  you to define a list of supported request types.
     *
     * @see EthJwtLoginProcessorProperties
     * @see LoginRequestProcess.RequestType
     */
    var rqTypes: List<LoginRequestProcess.RequestType> = listOf()

    /**
     * Holds a list of login request process types.
     */
    var types: List<LoginRequestProcess.TYPE> = listOf()
}
