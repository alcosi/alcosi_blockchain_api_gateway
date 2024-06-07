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

package com.alcosi.nft.apigateway.config.path.dto

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.predicate.FilterMatchPredicate
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType
import com.alcosi.nft.apigateway.service.predicate.SecurityConfigGatewayPredicate
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

/**
 * Represents the configuration for a security route.
 *
 * @param method The HTTP method for the route.
 * @param matches The list of filter match configurations for the route.
 * @param matchType The type of predicate matcher for the route.
 * @param type The type of predicate for the route.
 * @param basePath The base path for the route.
 * @param baseAuthorities The authorities for the base path of the route.
 * @param addBasePath Determines whether the base path should be added to
 *     the route path.
 */
data class SecurityRouteConfigDTO
@JsonCreator
constructor(
    val method: PathConfigurationComponent.Method,
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val matches: List<FilterMatchConfigDTO>,
    val matchType: PredicateMatcherType,
    val type: PathConfigurationComponent.PredicateType?,
    val basePath: String,
    val baseAuthorities: PathAuthorities,
    val addBasePath: Boolean,
) {
    /**
     * Converts the current instance of [SecurityRouteConfigDTO] to a [SecurityConfigGatewayPredicate].
     *
     * @return The resulting [SecurityConfigGatewayPredicate].
     */
    fun toPredicate(): SecurityConfigGatewayPredicate {
        val predicateType = type ?: PathConfigurationComponent.PredicateType.REGEX
        val prefix = if (addBasePath) basePath else ""
        val filterPredicate =
            FilterMatchPredicate(
                true,
                prefix,
                matches,
                predicateType,
                matchType,
                baseAuthorities,
            )
        return SecurityConfigGatewayPredicate(filterPredicate, this)
    }
}
