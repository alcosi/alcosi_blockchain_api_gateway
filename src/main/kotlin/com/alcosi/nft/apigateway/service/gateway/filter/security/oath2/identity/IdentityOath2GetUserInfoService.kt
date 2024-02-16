package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.lib.security.AccountDetails
import com.alcosi.lib.security.ClientAccountDetails
import com.alcosi.lib.security.PrincipalDetails
import com.alcosi.lib.security.UserDetails
import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.Oath2UserInfoProvider
import reactor.core.publisher.Mono

open class IdentityOath2GetUserInfoService(
    protected val claimClientId: String,
    protected val claimType: String,
    protected val claimAuthorities: String,
    protected val oath2GetUserInfoComponent: IdentityOath2GetUserInfoComponent,
    protected val oath2APIGetUserInfoComponent: IdentityOath2APIGetUserInfoComponent,
    protected val mappingHelper: MappingHelper,
) : Oath2UserInfoProvider {
    override fun getInfo(token: String): Mono<PrincipalDetails> {
        val timeStart = System.currentTimeMillis()
        var time = timeStart
        return oath2GetUserInfoComponent.getInfo(token)
            .flatMap {
                logger.debug("Oath2GetUserInfoComponent  getInfo took ${System.currentTimeMillis() - time}ms")
                time = System.currentTimeMillis()
                if (it.isError()) {
                    return@flatMap Mono.error(
                        ApiSecurityException(
                            "Error getting user details by token. Maybe it's outdated?",
                            it.error!!.httpCode.value(),
                        ),
                    )
                }
                val userMono = oath2APIGetUserInfoComponent.getInfo(it.response!!.id)
                return@flatMap userMono
            }.map { account ->
                logger.debug("oath2APIGetUserInfoComponent  getInfo took ${System.currentTimeMillis() - time}ms")
                logger.debug("Oath2GetUserInfoService  getInfo took ${System.currentTimeMillis() - timeStart}ms")
                val claims = account.claims
                val type = getType(claims)
                return@map when (type) {
                    "ACCOUNT" -> {
                        val clientId = getClientId(claims)
                        if (clientId == null) {
                            AccountDetails(account.id, getAuthorities(claims))
                        } else {
                            ClientAccountDetails(account.id, getAuthorities(claims), clientId)
                        }
                    }
                    "USER" -> UserDetails(account.id)
                    else -> throw ApiSecurityException("Account have bad type $type", 401201)
                }
            }
    }

    protected open fun getClientId(claims: List<IdentityOath2APIGetUserInfoComponent.User.Claim>) = claims.firstOrNull { it.type.equals(claimClientId, true) }?.value

    protected open fun getType(claims: List<IdentityOath2APIGetUserInfoComponent.User.Claim>) = (claims.firstOrNull { it.type.equals(claimType, true) }?.value) ?: "USER"

    protected open fun getAuthorities(claims: List<IdentityOath2APIGetUserInfoComponent.User.Claim>): List<String> {
        return claims.filter { it.type.equals(claimAuthorities, true) }
            .flatMap {
                if (!it.value.contains("[")) {
                    listOf(it.value)
                } else {
                    mappingHelper.mapList(it.value, String::class.java)
                }
            }
    }
}