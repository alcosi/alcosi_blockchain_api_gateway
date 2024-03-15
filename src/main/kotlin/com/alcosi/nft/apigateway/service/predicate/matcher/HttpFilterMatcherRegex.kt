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

class HttpFilterMatcherRegex(prefix: String, config: FilterMatchConfigDTO) : HttpFilterMatcher<Regex>(prefix, config) {
    override val predicateType: PathConfigurationComponent.PredicateType = PathConfigurationComponent.PredicateType.REGEX

    override fun checkUri(uri: String): Boolean {
        return matcher.matches(uri)
    }

    override fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): Regex {
        return "$prefix${config.path}".replace("/", "\\/").toRegex()
    }
}
