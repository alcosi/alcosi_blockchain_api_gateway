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

class PathAuthorities(val pathAuthorityList: List<PathAuthority>,
                      val checkMode: AuthoritiesCheck = AuthoritiesCheck.ALL) {
    enum class AuthoritiesCheck {
        ANY,
        ALL,
    }
    fun checkHaveAuthorities(profileAuth: List<String>?): Boolean {
        return when(checkMode){
            AuthoritiesCheck.ANY ->pathAuthorityList.any { it.checkHaveAuthorities(profileAuth) }
            AuthoritiesCheck.ALL ->pathAuthorityList.all { it.checkHaveAuthorities(profileAuth) }
        }
    }

    fun haveAuth(): Boolean {
        return pathAuthorityList.any { it.haveAuth() }
    }

    fun noAuth(): Boolean {
        return !haveAuth()
    }
}
