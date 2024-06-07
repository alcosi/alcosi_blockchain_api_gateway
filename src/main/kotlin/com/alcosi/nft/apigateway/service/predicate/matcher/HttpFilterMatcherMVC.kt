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
import org.springframework.http.server.PathContainer
import org.springframework.web.util.pattern.PathPattern
import org.springframework.web.util.pattern.PathPatternParser

/**
 * This class represents an HTTP filter matcher for MVC-style path matching.
 *
 * @param prefix The prefix to be used for matching.
 * @param config The filter match configuration.
 */
open class HttpFilterMatcherMVC(prefix: String, config: FilterMatchConfigDTO) : HttpFilterMatcher<PathPattern>(prefix, config) {
    /**
     * Represents the type of predicate used in the PathConfigurationComponent class.
     *
     * The available types are:
     * - MVC: Matches routes using the Model-View-Controller design pattern.
     * - REGEX: Matches routes using regular expressions.
     */
    override val predicateType: PathConfigurationComponent.PredicateType = PathConfigurationComponent.PredicateType.MVC

    /**
     * Checks if the given URI matches the criteria for filtering HTTP requests.
     *
     * @param uri The URI to be checked.
     * @return true if the URI matches the criteria, false otherwise.
     */
    override fun checkUri(uri: String): Boolean {
        return matcher.matches(PathContainer.parsePath(uri))
    }

    /**
     * Creates a PathPattern by parsing the prefix and path from the FilterMatchConfigDTO.
     *
     * @param prefix The prefix to be used for matching.
     * @param config The filter match configuration containing the path.
     * @return The created PathPattern.
     */
    override fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): PathPattern {
        return pathPatternParser.parse("$prefix${config.path}")
    }

    /**
     * This class represents a companion object for the HttpFilterMatcherMVC class.
     *
     * It contains a single property, `pathPatternParser`, which represents an instance
     * of the `PathPatternParser` class used to parse path patterns for HTTP filter matching.
     * The `pathPatternParser` property is instantiated using a default constructor,
     * and it is used in conjunction with the `HttpFilterMatcherMVC` class to create a `PathPattern`
     * object by parsing the prefix and path from the `FilterMatchConfigDTO` object.
     *
     * @see PathPatternParser
     * @see HttpFilterMatcherMVC
     */
    companion object {
        /**
         * This variable represents an instance of the `PathPatternParser` class,
         * which is used to parse path patterns for HTTP filter matching.
         *
         * It is instantiated using a default constructor:
         * The `PathPatternParser` class is part of a larger codebase and contains parsing logic
         * to convert a path string into a `PathPattern` object.
         * It is used in conjunction with the `HttpFilterMatcherMVC` class to create a `PathPattern`
         * by parsing the prefix and path from the `FilterMatchConfigDTO` object.
         *
         * @see PathPatternParser
         * @see HttpFilterMatcherMVC
         */
        val pathPatternParser = PathPatternParser()
    }
}
