package com.alcosi.nft.apigateway.config.path.dto

class PathAuthorities(val list: List<PathAuthority>) {
    fun checkHaveAuthorities(profileAuth: List<String>?): Boolean {
        return list.all { it.checkHaveAuthorities(profileAuth) }
    }

    fun haveAuth(): Boolean {
        return list.any { it.haveAuth() }
    }

    fun noAuth(): Boolean {
        return !haveAuth()
    }
}
