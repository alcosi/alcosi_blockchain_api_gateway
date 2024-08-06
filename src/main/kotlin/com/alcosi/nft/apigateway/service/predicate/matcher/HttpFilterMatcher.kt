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
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent.Companion.ATTRIBUTE_ALLOWED_HTTP_METHODS_SET
import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO
import org.springframework.http.HttpMethod
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange

/**
 * Provides an abstract base class for implementing HTTP filter matchers.
 *
 * @param T The type of the matcher to be created.
 * @property prefix The prefix to be used for matching.
 * @property config The filter match configuration.
 */
abstract class HttpFilterMatcher<T>(
   protected val prefix: String,
   val config: FilterMatchConfigDTO,
) {
    /**
     * Provides a lazy-initialized property for a matcher that is used for filtering requests.
     *
     * @param T The type of the matcher.
     * @property prefix The prefix to be used for matching.
     * @property config The filter match configuration.
     * @property matcher The lazy-initialized property for the matcher.
     */
    val matcher: T by lazy { createMather(prefix, config) }

    /**
     * Provides an abstract base class for implementing HTTP filter matchers.
     */
    abstract val predicateType: PathConfigurationComponent.PredicateType

    /**
     * Provides a list of HTTP methods for filtering requests.
     *
     * @property methods The list of HTTP methods to be considered for filtering.
     */
    val methods: List<HttpMethod> = config.methods

    /**
     * Creates a matcher for filtering HTTP requests based on the provided prefix and configuration.
     *
     * @param prefix The prefix to be used for matching.
     * @param config The filter match configuration containing the methods, path, authorities groups, authorities check mode, and order.
     * @return The created matcher for filtering requests.
     */
    abstract fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): T

    /**
     * Checks if the given request matches the criteria for filtering HTTP requests.
     *
     * @param requestExchange The ServerWebExchange object representing the request.
     * @return true if the request matches the criteria, false otherwise.
     */
    open fun checkRequest(
        requestExchange: ServerWebExchange,
    ): Boolean {
        val uri=requestExchange.request.path.toString()
        val method=requestExchange.request.method
        val pathMatches = checkUri(uri)
        if (!pathMatches) {
            return false
        }
        if (pathMatches){
            (requestExchange.attributes[ATTRIBUTE_ALLOWED_HTTP_METHODS_SET] as MutableSet<HttpMethod>).addAll(methods)
        }
        val methodMatches = checkMethod(method)
        return methodMatches
    }

    /**
     * Checks if the given HTTP method is contained in the list of supported methods.
     *
     * @param method The HTTP method to check.
     * @return true if the method is supported, false otherwise.
     */
    open fun checkMethod(method: HttpMethod): Boolean {
        return methods.contains(method)
    }

    /**
     * Checks if the given URI matches the criteria for filtering HTTP requests.
     *
     * @param uri The URI to be checked.
     * @return true if the URI matches the criteria, false otherwise.
     */
    abstract fun checkUri(uri: String): Boolean
}
