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

import com.alcosi.lib.security.PrincipalDetails
import org.apache.logging.log4j.kotlin.Logging
import reactor.core.publisher.Mono

/**
 * Oath2UserInfoProvider interface provides a method to retrieve user information based on an OAuth2 token.
 */
interface Oath2UserInfoProvider : Logging {
    /**
     * Retrieves user information based on an OAuth2 token.
     *
     * @param token The token used for authentication.
     * @return A Mono that emits the user information as a PrincipalDetails object.
     */
    fun getInfo(token: String): Mono<PrincipalDetails>
}
