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
package com.alcosi.nft.apigateway.config.path

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent.PredicateType
import com.alcosi.nft.apigateway.config.path.dto.PathAuthority
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The SecurityRoutesProperties class represents a configuration properties for security routes in the application.
 *
 * @constructor Creates an instance of SecurityRoutesProperties class.
 */
@ConfigurationProperties("filter.config.path")
open class SecurityRoutesProperties {
    /**
     * The SecurityRoutesProperties class represents a configuration properties for security routes in the application.
     */
    var security: SecurityRouteConfig = SecurityRouteConfig()

    /**
     * The `validation` variable represents the configuration for validating security routes in the application.
     * It is an instance of the `SecurityRouteConfig` class defined in the `SecurityRoutesProperties` class.
     *
     * @see SecurityRoutesProperties
     * @see SecurityRouteConfig
     */
    var validation: SecurityRouteConfig = SecurityRouteConfig()

    /**
     * The Type class represents a type with method, match, and predicate properties.
     */
    open class Type {
        /**
         * The method variable represents the selected method in the PathConfigurationComponent class.
         *
         * @property method The selected method.
         * @see PathConfigurationComponent
         */
        var method: PathConfigurationComponent.Method = PathConfigurationComponent.Method.ETH_JWT

        /**
         * The `match` variable is of type `PredicateMatcherType`. It represents the type of predicate matcher used in the `PathConfigurationComponent` class.
         *
         * Available options for `PredicateMatcherType`:
         *  - MATCH_IF_NOT_CONTAINS_IN_LIST: Matches if the value does not contain in the list.
         *  - MATCH_IF_CONTAINS_IN_LIST: Matches if the value contains in the list.
         *
         * The default value of `match` is `MATCH_IF_CONTAINS_IN_LIST`.
         *
         * Example usage:
         *
         * ```kotlin
         * var match: PredicateMatcherType = PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST
         * ```
         */
        var match: PredicateMatcherType = PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST

        /**
         * The type of predicate used in the PathConfigurationComponent class.
         *
         * The PredicateType enum defines the possible types of predicates that can be used to match routes
         * in the PathConfigurationComponent class.
         */
        var predicate: PredicateType = PredicateType.MVC
    }

    /**
     * The SecurityRouteConfig class represents the configuration for security routes.
     */
    open class SecurityRouteConfig {
        /**
         * The `path` variable represents a mutable map of string keys to string (json) values.
         *
         * @property path The path represented as a mutable map of string keys to string values.
         *
         * Note: The variable is declared in the containing class `SecurityRouteConfig`.
         *
         * @see SecurityRouteConfig
         */
        var path: MutableMap<String, String> = mutableMapOf()

        /**
         * The `type` variable represents an instance of the `Type` class.
         *
         * @property method The selected method in the `PathConfigurationComponent` class.
         * @property match The type of predicate matcher used in the `PathConfigurationComponent` class.
         * @property predicate The type of predicate used in the `PathConfigurationComponent` class.
         *
         * Note: The variable is declared in the `SecurityRouteConfig` class.
         *
         * @see Type
         * @see PathConfigurationComponent
         */
        var type: Type = Type()

        /**
         * The baseAuthorities variable represents the base authorities for security routes.
         *
         * @property baseAuthorities The base authorities as a JSON string in the format: [{"list":["ALL"],"checkMode":"ALL"}]
         *
         * @see PathAuthority
         */
        var baseAuthorities: String = "[{\"list\":[\"ALL\"],\"checkMode\":\"ALL\"}]"

        /**
         * The `baseAuthoritiesCheckType` variable represents the check mode for the base authorities in security routes.
         *
         * @property baseAuthoritiesCheckType The check mode for the base authorities.
         *
         * @see PathAuthority.AuthoritiesCheck
         */
        var baseAuthoritiesCheckType: PathAuthority.AuthoritiesCheck = PathAuthority.AuthoritiesCheck.ANY
    }
}
