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

class HttpFilterMatcherMVC(prefix: String, config: FilterMatchConfigDTO) : HttpFilterMatcher<PathPattern>(prefix, config) {
    override val predicateType: PathConfigurationComponent.PredicateType = PathConfigurationComponent.PredicateType.MVC

    override fun checkUri(uri: String): Boolean {
        return matcher.matches(PathContainer.parsePath(uri))
    }

    override fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): PathPattern {
        return pathPatternParser.parse("$prefix${config.path}")
    }

    companion object {
        val pathPatternParser = PathPatternParser()
    }
}
