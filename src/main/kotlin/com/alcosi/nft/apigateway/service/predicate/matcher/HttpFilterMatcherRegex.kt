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

/**
 * Provides a class for filtering HTTP requests based on a regular expression match.
 *
 * @param prefix The prefix to be used for matching.
 * @param config The filter match configuration.
 */
open class HttpFilterMatcherRegex(prefix: String, config: FilterMatchConfigDTO) : HttpFilterMatcher<Regex>(prefix, config) {
    /**
     * Represents the type of predicate used in the [PathConfigurationComponent] class.
     *
     * The [PredicateType] enum defines the possible types of predicates that can be used to match routes in the [PathConfigurationComponent] class.
     * The available types are:
     * - [PredicateType.MVC]: Matches routes using the Model-View-Controller design pattern.
     * - [PredicateType.REGEX]: Matches routes using regular expressions.
     */
    override val predicateType: PathConfigurationComponent.PredicateType = PathConfigurationComponent.PredicateType.REGEX

    /**
     * Checks if the given URI matches the criteria for filtering HTTP requests.
     *
     * @param uri The URI to be checked.
     * @return true if the URI matches the criteria, false otherwise.
     */
    override fun checkUri(uri: String): Boolean {
        return matcher.matches(uri)
    }

    /**
     * Creates a matcher for filtering HTTP requests based on the provided prefix and configuration.
     *
     * @param prefix The prefix to be used for matching.
     * @param config The filter match configuration.
     * @return The created matcher as a Regex object.
     */
    override fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): Regex {
        return "$prefix${config.path}".replace("/", "\\/").toRegex()
    }
}
