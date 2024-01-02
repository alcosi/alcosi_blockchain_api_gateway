package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.alcosi.lib.security.AccountDetails
import com.alcosi.lib.security.PrincipalDetails
import com.alcosi.lib.security.UserDetails
import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException
import org.apache.logging.log4j.kotlin.logger
import reactor.core.publisher.Mono

open class Oath2GetUserInfoService(
    protected val oath2GetUserInfoComponent: Oath2GetUserInfoComponent,
    protected val oath2APIGetUserInfoComponent: Oath2APIGetUserInfoComponent
) {


    open fun getInfo(token: String): Mono<PrincipalDetails> {
        val timeStart=System.currentTimeMillis()
        var time=timeStart
        return oath2GetUserInfoComponent.getInfo(token).flatMap {
            logger.debug("Oath2GetUserInfoComponent  getInfo took ${System.currentTimeMillis()-time}ms")
            time=System.currentTimeMillis()
            if (it.isError()) {
                return@flatMap Mono.error(
                    ApiSecurityException(
                        "Error getting user details by token. Maybe it's outdated?",
                        it.error!!.httpCode.value()
                    )
                )
            }
            return@flatMap oath2APIGetUserInfoComponent.getInfo(it.response!!.id)
        }.map { account ->
            logger.debug("oath2APIGetUserInfoComponent  getInfo took ${System.currentTimeMillis()-time}ms")
            logger.debug("Oath2GetUserInfoService  getInfo took ${System.currentTimeMillis()-timeStart}ms")
            val claims = account.claims.associate { it.type to it.value }
            val type = getType(claims)
            return@map when (type) {
                "ACCOUNT" -> ClientAccountDetails(account.id, getAuthorities(claims), getClientId(claims))
                "USER" -> UserDetails(account.id)
                else -> throw ApiSecurityException("Account have bad type $type", 401201)
            }
        }

    }

    protected open fun getClientId(claims: Map<String, String>) = claims["clientId"]

    protected open fun getType(claims: Map<String, String>) = claims["type"]

    protected open fun getAuthorities(claims: Map<String, String>) =
        claims["authorities"]?.split(" ") ?: listOf()
}