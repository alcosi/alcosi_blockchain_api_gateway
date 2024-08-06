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
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent.Companion.ATTRIBUTE_ALLOWED_HTTP_METHODS_SET
import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO
import com.alcosi.nft.apigateway.config.path.dto.PathAuthorities
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcher
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherMVC
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherRegex
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.HttpMethod
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

/**
 * FilterMatchPredicate is a base class for filter match predicates used in request filtering.
 *
 * @param isSecured Specifies whether the predicate is secured.
 * @param prefix The prefix used for matching paths.
 * @param pathMethods The list of FilterMatchConfigDTO objects representing the configuration for filtering matching requests.
 * @param type The type of predicate.
 * @param matchType The type of predicate matcher.
 * @param baseAuthorities The base authorities to be used for the predicate.
 */
open class FilterMatchPredicate(
    val isSecured: Boolean,
    prefix: String,
    pathMethods: List<FilterMatchConfigDTO>,
    val type: PathConfigurationComponent.PredicateType,
    val matchType: PredicateMatcherType,
    val baseAuthorities: PathAuthorities,
) : Logging, Predicate<ServerWebExchange> {
    /**
     * Represents a list of HTTP filter matchers.
     *
     * @property matchers The list of HTTP filter matchers.
     */
    open val matchers: List<HttpFilterMatcher<*>> =
        when (type) {
            PathConfigurationComponent.PredicateType.REGEX -> pathMethods.map { HttpFilterMatcherRegex(prefix, it) }
            PathConfigurationComponent.PredicateType.MVC -> pathMethods.map { HttpFilterMatcherMVC(prefix, it) }
        }.sortedBy { it.config }

    /**
     * Finds the best matching HTTP filter matcher for the given ServerWebExchange.
     *
     * @param exchange The ServerWebExchange to be checked.
     * @return The best matching HttpFilterMatcher or null if no match is found.
     */
    open fun findMatcher(exchange: ServerWebExchange): HttpFilterMatcher<*>? {
        exchange.attributes[ATTRIBUTE_ALLOWED_HTTP_METHODS_SET]= mutableSetOf<HttpMethod>()
        val matcher = matchers.find { it.checkRequest(exchange) }
        return matcher
    }

    /**
     * Sets the matcher attribute for the given HttpFilterMatcher and ServerWebExchange.
     *
     * @param matcher The HttpFilterMatcher to set the attribute for.
     * @param exchange The ServerWebExchange to set the attribute on.
     */
    protected open fun setMatcherAttribute(
        matcher: HttpFilterMatcher<*>?,
        exchange: ServerWebExchange,
    ) {
        if (matcher?.config?.authorities()?.haveAuth() == true) {
            exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] = matcher.config.authorities()
        } else {
            if (isSecured) {
                if (matcher != null || matchType == PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST) {
                    exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] = baseAuthorities
                }
            }
        }
    }

    /**
     * Tests the ServerWebExchange against the configured criteria to determine if it matches.
     *
     * @param t The ServerWebExchange to be tested.
     * @return true if the ServerWebExchange matches the criteria, false otherwise.
     */
    override fun test(t: ServerWebExchange): Boolean {
        val matcher = findMatcher(t)
        setMatcherAttribute(matcher, t)
        return when (matchType) {
            PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST -> matcher != null
            PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST -> matcher == null
        }
    }

}
