package com.alcosi.nft.apigateway.config.path.dto

data class PathAuthority(val list: List<String>, val checkMode: AuthoritiesCheck = AuthoritiesCheck.ANY) {
    enum class AuthoritiesCheck {
        ANY,
        ALL,
    }

    fun noAuth(): Boolean {
        return list.isNullOrEmpty()
    }

    fun haveAuth(): Boolean {
        return !noAuth()
    }

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
