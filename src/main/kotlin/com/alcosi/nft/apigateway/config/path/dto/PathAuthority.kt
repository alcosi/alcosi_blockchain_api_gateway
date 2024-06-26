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
 * The `PathAuthority` class represents a path authority and provides
 * methods for checking the authority against a given profile.
 *
 * @param list The list of authorities.
 * @param checkMode The authorities check mode.
 * @constructor Creates a new instance of the `PathAuthority` class.
 */
data class PathAuthority(val list: List<String>, val checkMode: AuthoritiesCheck = AuthoritiesCheck.ANY) {
    /** The `AuthoritiesCheck` enum represents the check mode for authorities. */
    enum class AuthoritiesCheck {
        ANY,
        ALL,
    }

    /**
     * Determines*/
    fun noAuth(): Boolean {
        return list.isNullOrEmpty()
    }

    /**
     * Determines whether the path authorities have authentication.
     *
     * @return true if the path authorities have authentication, false otherwise.
     */
    fun haveAuth(): Boolean {
        return !noAuth()
    }

    /**
     * Checks whether the given profile authorities satisfy the path authorities based on the check mode.
     *
     * @param profileAuth The list of profile authorities.
     * @return true if the profile authorities satisfy the path authorities, false otherwise.
     */
    fun checkHaveAuthorities(profileAuth: List<String>?): Boolean {
        if (noAuth()) {
            return true
        } else if (profileAuth.isNullOrEmpty()) {
            return false
        } else {
            return when (checkMode) {
                AuthoritiesCheck.ALL -> list.all { profileAuth.contains(it) }
                AuthoritiesCheck.ANY -> list.any { profileAuth.contains(it) }
            }
        }
    }
}
