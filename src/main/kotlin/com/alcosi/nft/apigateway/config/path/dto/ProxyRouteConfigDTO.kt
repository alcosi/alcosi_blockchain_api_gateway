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
import com.alcosi.nft.apigateway.service.predicate.RouteConfigGatewayPredicate
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

/**
 * Represents the configuration for a proxy route in the application.
 *
 * @property name The name of the proxy route.
 * @property matches The list of filter match configurations for matching requests.
 * @property microserviceUri The URI of the microservice to which requests should be proxied.
 * @property matchType The type of matching for the filter.
 * @property type The type of predicate used for matching.
 * @property basePathFilter Indicates whether the base path should be filtered.
 * @property order The order of the proxy route configuration.
 * @property basePath The base path for filtering requests.
 * @property addBasePath Indicates whether the base path should be appended to the proxied request.
 * @property encryptFields The list of fields to encrypt in the request.
 * @property apiKey The API key for authenticating requests.
 */
data class ProxyRouteConfigDTO
    @JsonCreator
    constructor(
        val name: String?,
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
        val matches: List<FilterMatchConfigDTO>,
        val microserviceUri: String,
        val matchType: PredicateMatcherType?,
        val type: PathConfigurationComponent.PredicateType?,
        val basePathFilter: Boolean?,
        val order: Int?,
        val basePath: String?,
        val addBasePath: Boolean?,
        val encryptFields: List<String>?,
        val apiKey: String?,
        val logBody:Boolean = true,
        val logHeaders:Boolean = true,
        val convertMultipartToJson:Boolean=true
    ) {
    /**
     * Returns a [RouteConfigGatewayPredicate] based on the current state of the ProxyRouteConfigDTO object.
     *
     * If the 'type' property is null, the default value [PathConfigurationComponent.PredicateType.MVC] is used.
     * If 'addBasePath' is false, the 'prefix' is set to an empty string. Otherwise, it is set to the value of 'basePath'.
     * The 'matches' property is used as the 'pathMethods' argument for the FilterMatchPredicate constructor.
     * The 'matchType' property is used as the 'matchType' argument for the FilterMatchPredicate constructor.
     * The FilterMatchPredicate is created with a new instance of PathAuthorities containing a single PathAuthority with an empty list of authorities.
     * Finally, a new RouteConfigGatewayPredicate is created with the FilterMatchPredicate and the current ProxyRouteConfigDTO object, and returned.
     *
     * @return The generated [RouteConfigGatewayPredicate] based on the current state of the ProxyRouteConfigDTO object.
     */
    fun toPredicate(): RouteConfigGatewayPredicate {
            val predicateType = type ?: PathConfigurationComponent.PredicateType.MVC
            val prefix = if (addBasePath == false) "" else (basePath ?: "")
            val filterPredicate =
                FilterMatchPredicate(
                    false,
                    prefix,
                    matches,
                    predicateType,
                    matchType ?: PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST,
                    PathAuthorities(listOf(PathAuthority(listOf(), PathAuthority.AuthoritiesCheck.ANY))),
                )
            return RouteConfigGatewayPredicate(filterPredicate, this)
        }
    }
