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

package com.alcosi.nft.apigateway.service.predicate.matcher

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO
import org.springframework.http.HttpMethod

abstract class HttpFilterMatcher<T>(
    val prefix: String,
    val config: FilterMatchConfigDTO,
) {
    val matcher: T by lazy { createMather(prefix, config) }
    abstract val predicateType: PathConfigurationComponent.PredicateType
    val methods: List<HttpMethod> = config.methods

    abstract fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): T

    open fun checkRequest(
        uri: String,
        method: HttpMethod,
    ): Boolean {
        val methodMatches = checkMethod(method)
        if (!methodMatches) {
            return false
        }
        val pathMatches = checkUri(uri)
        return pathMatches
    }

    open fun checkMethod(method: HttpMethod): Boolean {
        return methods.contains(method)
    }

    abstract fun checkUri(uri: String): Boolean
}
