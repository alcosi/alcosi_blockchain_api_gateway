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

/**
 * Data class representing the configuration for filtering matching
 * requests.
 *
 * @property methods The list of HTTP methods that should be considered for
 *     matching.
 * @property path The path that should be considered for matching.
 * @property authoritiesGroups The list of authorities groups or
 *     authorities that should be considered for matching.
 * @property authoritiesGroupsCheckMode The check mode for authorities
 *     groups or authorities.
 * @property order The order of the filter match configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class FilterMatchConfigDTO
@JsonCreator
constructor(
    val methods: List<HttpMethod>,
    val path: String,
    @JsonAlias("authoritiesGroups", "authorities")
    private val authoritiesGroups: List<PathAuthority>?,
    @JsonAlias("authoritiesGroupsCheckMode", "authoritiesCheckMode")
    private val authoritiesGroupsCheckMode: PathAuthorities.AuthoritiesCheck?,
    private val order: Int?,
) : Ordered, Comparable<FilterMatchConfigDTO> {
    /**
     * Retrieves the authorities for the given path.
     *
     * @return The pathAuthorities that have been set for the filter match configuration.
     */
    fun authorities(): PathAuthorities {
        return if (authoritiesGroups == null) PathAuthorities(listOf()) else PathAuthorities(authoritiesGroups, authoritiesGroupsCheckMode ?: PathAuthorities.AuthoritiesCheck.ALL)
    }

    /**
     * Returns the order of the filter match configuration.
     *
     * @return The order of the filter match configuration.
     */
    override fun getOrder(): Int {
        return order ?: 0
    }

    /**
     * Compares this FilterMatchConfigDTO object with the specified object for order.
     * The comparison is based on the order property of each object.
     *
     * @param other The FilterMatchConfigDTO object to be compared.
     * @return A negative integer, zero, or a positive integer as this object is less than,
     *         equal to, or greater than the specified object.
     */
    override fun compareTo(other: FilterMatchConfigDTO): Int {
        return getOrder().compareTo(other.getOrder())
    }
}
