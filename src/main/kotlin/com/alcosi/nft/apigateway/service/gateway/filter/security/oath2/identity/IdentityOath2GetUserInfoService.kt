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

package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity

import com.alcosi.lib.objectMapper.mapList
import com.alcosi.lib.security.*
import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.Oath2UserInfoProvider
import com.fasterxml.jackson.databind.ObjectMapper
import reactor.core.publisher.Mono

/**
 * The IdentityOath2GetUserInfoService is a class that provides the ability to retrieve user information
 * from the identity server using OAuth2.
 *
 * @property claimClientId The claim indicating the client ID.
 * @property claimOrganisationId The claim indicating the organisation ID.
 * @property claimType The claim indicating the user type.
 * @property claimAuthorities The claim indicating the user authorities.
 * @property oath2GetUserInfoComponent The component used to retrieve user information using OAuth2.
 * @property oath2APIGetUserInfoComponent The component used to retrieve user information using the OAuth2 API.
 * @property mappingHelper The MappingHelper instance used for object mapping.
 */
open class IdentityOath2GetUserInfoService(
    protected val claimClientId: String,
    protected val claimOrganisationId: String,
    protected val claimType: String,
    protected val claimAuthorities: String,
    protected val oath2GetUserInfoComponent: IdentityOath2GetUserInfoComponent,
    protected val oath2APIGetUserInfoComponent: IdentityOath2APIGetUserInfoComponent,
    protected val mappingHelper: ObjectMapper,
) : Oath2UserInfoProvider {
    /**
     * Retrieves user information based on the provided token.
     *
     * @param token The access token of the user.
     * @return A Mono that emits the PrincipalDetails object.
     * @throws ApiSecurityException if there is an error getting user details by token.
     */
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
                        val organisationId = getOrganisationId(claims)
                        if (organisationId != null) {
                            return@map OrganisationAccountDetails(account.id, getAuthorities(claims), organisationId,  OrganisationAccountDetails::class.java.name)
                        }
                        val clientId = getClientId(claims)
                        if (clientId != null) {
                            return@map ClientAccountDetails(account.id, getAuthorities(claims), clientId, ClientAccountDetails::class.java.name)
                        } else {
                            return@map AccountDetails(account.id, getAuthorities(claims), AccountDetails::class.java.name)
                        }
                    }

                    "USER" -> UserDetails(account.id,UserDetails::class.java.name)
                    else -> throw ApiSecurityException("Account have bad type $type", 401201)
                }
            }
    }

    /**
     * Retrieves the client ID from the list of claims.
     *
     * @param claims The list of claims.
     * @return The client ID or null if no matching claim is found.
     */
    protected open fun getClientId(claims: List<IdentityOath2APIGetUserInfoComponent.User.Claim>) = claims.firstOrNull { it.type.equals(claimClientId, true) }?.value

    /**
     * Retrieves the organization ID from the list of claims.
     *
     * @param claims The list of claims.
     * @return The organization ID or null if no matching claim is found.
     */
    protected open fun getOrganisationId(claims: List<IdentityOath2APIGetUserInfoComponent.User.Claim>) = claims.firstOrNull { it.type.equals(claimOrganisationId, true) }?.value

    /**
     * Retrieves the type of a claim from the given list of claims.
     *
     * @param claims The list of claims from which to retrieve the type.
     * @return The type of the first matching claim, or "USER" if no matching claim is found.
     */
    protected open fun getType(claims: List<IdentityOath2APIGetUserInfoComponent.User.Claim>) = (claims.firstOrNull { it.type.equals(claimType, true) }?.value) ?: "USER"

    /**
     * Retrieves the authorities from a given list of user claims.
     *
     * @param claims The list of user claims.
     * @return The list of authorities extracted from the claims.
     */
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
