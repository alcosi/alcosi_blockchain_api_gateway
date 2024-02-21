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
