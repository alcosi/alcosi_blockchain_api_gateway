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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
@JsonIgnoreProperties(ignoreUnknown = true)
data class FilterMatchConfigDTO
    @JsonCreator
    constructor(
        val methods: List<HttpMethod>,
        val path: String,
        @JsonAlias("authoritiesGroups","authorities")
        private val authoritiesGroups: List<PathAuthority>?,
        @JsonAlias("authoritiesGroupsCheckMode","authoritiesCheckMode")
        private val authoritiesGroupsCheckMode: PathAuthorities.AuthoritiesCheck?,
        private val order: Int?,
    ) : Ordered, Comparable<FilterMatchConfigDTO> {
        fun authorities(): PathAuthorities {
            return if (authoritiesGroups == null) PathAuthorities(listOf()) else PathAuthorities(authoritiesGroups,authoritiesGroupsCheckMode?:PathAuthorities.AuthoritiesCheck.ALL)
        }

        override fun getOrder(): Int {
            return order ?: 0
        }

        override fun compareTo(other: FilterMatchConfigDTO): Int {
            return getOrder().compareTo(other.getOrder())
        }
    }
