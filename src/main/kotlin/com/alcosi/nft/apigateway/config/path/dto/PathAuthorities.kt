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

/**
 * The `PathAuthorities` class represents a collection of path authorities
 * and provides methods for checking the authorities against a given
 * profile.
 *
 * @param pathAuthorityList The list of path authorities.
 * @param checkMode The authorities check mode.
 * @constructor Creates a new instance of the `PathAuthorities` class.
 */
open class PathAuthorities(
    val pathAuthorityList: List<PathAuthority>,
    val checkMode: AuthoritiesCheck = AuthoritiesCheck.ALL
) {
    /**
     * The `AuthoritiesCheck` enum represents the check modes for authorities.
     * The check modes can be ANY or ALL.
     */
    enum class AuthoritiesCheck {
        ANY,
        ALL,
    }

    /**
     * Checks whether the given profile authorities satisfy the path
     * authorities based on the check mode.
     *
     * @param profileAuth The list of profile authorities.
     * @return true if the profile authorities satisfy the path authorities,
     *     false otherwise.
     */
    open fun checkHaveAuthorities(profileAuth: List<String>?): Boolean {
        return when (checkMode) {
            AuthoritiesCheck.ANY -> pathAuthorityList.any { it.checkHaveAuthorities(profileAuth) }
            AuthoritiesCheck.ALL -> pathAuthorityList.all { it.checkHaveAuthorities(profileAuth) }
        }
    }

    /**
     * Checks whether the path authorities have authentication.
     *
     * @return true if the path authorities have authentication, false
     *     otherwise.
     */
    open fun haveAuth(): Boolean {
        return pathAuthorityList.any { it.haveAuth() }
    }

    /**
     * Checks whether the path authorities have authentication.
     *
     * @return true if the path authorities have authentication, false
     *     otherwise.
     */
    fun noAuth(): Boolean {
        return !haveAuth()
    }
}
