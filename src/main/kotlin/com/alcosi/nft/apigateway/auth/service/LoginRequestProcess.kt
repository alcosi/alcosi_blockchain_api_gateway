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

import org.springframework.http.HttpMethod
import reactor.core.publisher.Mono

/**
 * Interface representing the login request process.
 */
interface LoginRequestProcess {
    /**
     * Returns a list of RequestType values.
     *
     * @return the list of RequestType values
     */
    fun rqTypes(): List<RequestType>

    /**
     * Returns a list of TYPE values.
     *
     * @return the list of TYPE values
     */

    fun types(): List<TYPE>

    /**
     * Returns the HTTP method used by the login request process.
     *
     * @return the HTTP method used by the login request process
     */

    fun method(): HttpMethod

    /**
     * Enum representing the type of request.
     */
    enum class RequestType { GET, POST, PUT }

    /**
     * Represents the types for a login request process.
     */
    enum class TYPE { BEFORE, AFTER }

    /**
     * Processes the given wallet.
     *
     * @param wallet the wallet to be processed
     * @return a Mono representing the completion of the process
     */
    fun process(wallet: String): Mono<Void>
}
