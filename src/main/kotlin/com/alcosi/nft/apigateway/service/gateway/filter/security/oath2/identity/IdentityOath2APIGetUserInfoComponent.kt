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

import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.Oath2AuthComponent
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * The IdentityOath2APIGetUserInfoComponent is a class that allows retrieving user information
 * from the identity server using the OAuth2 API.
 *
 * @property mappingHelper The MappingHelper instance used for object mapping.
 * @property webClient The WebClient instance used for making HTTP requests.
 * @property oath2AuthComponent The Oath2AuthComponent instance used for OAuth2 authentication.
 * @property apiVersion The version of the API.
 * @property idServerUri The URI of the identity server.
 * @property relativePath The relative path for retrieving user information.
 */
open class IdentityOath2APIGetUserInfoComponent(
    protected val mappingHelper: ObjectMapper,
    protected val webClient: WebClient,
    protected val oath2AuthComponent: Oath2AuthComponent,
    protected val apiVersion: String,
    idServerUri: String,
    relativePath: String = "/api/user/{id}",
) {
    /**
     * The URI for retrieving user information.
     */
    protected open val getUserInfoUri = "${idServerUri}$relativePath"

    /**
     * Represents a User.
     *
     * @property id The user's ID.
     * @property fullName The user's full name.
     * @property email The user's email.
     * @property phoneNumber The user's phone number.
     * @property claims The user's claims.
     * @property photo The user's photo.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class User
        @JsonCreator
        constructor(
            @JsonProperty("id")
            val id: String,
            @JsonProperty("fullName")
            val fullName: FullName?,
            @JsonProperty("email")
            val email: String?,
            @JsonProperty("phoneNumber")
            val phoneNumber: String?,
            val claims: List<Claim>,
            val photo: String?,
        ) {
        /**
         * Represents a full name.
         *
         * @property firstName The first name.
         * @property lastName The last name.
         * @property middleName The middle name.
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
            data class FullName
                @JsonCreator
                constructor(
                    @JsonProperty("firstName")
                    val firstName: String?,
                    @JsonProperty("lastName")
                    val lastName: String?,
                    @JsonProperty("JsonProperty")
                    val middleName: String?,
                )

        /**
         * Represents a claim in the user's profile.
         *
         * @property type The type of the claim.
         * @property value The value of the claim.
         */
        data class Claim
                @JsonCreator
                constructor(
                    @JsonProperty("type") val type: String,
                    @JsonProperty("value") val value: String,
                )
        }

    /**
     * Retrieves user information based on the provided ID.
     *
     * @param id The ID of the user.
     * @return A Mono that emits the User object.
     * @throws ApiException if there is an error retrieving the user information.
     */
    fun getInfo(id: String): Mono<User> {
        return webClient
            .get()
            .uri(getUserInfoUri.replace("{id}", id))
            .header("Authorization", "Bearer ${oath2AuthComponent.getAccessToken()}")
            .header("x-api-version", apiVersion)
            .exchangeToMono { rs ->
                if (!rs.statusCode().is2xxSuccessful) {
                    return@exchangeToMono rs.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map {
                            throw ApiException(500000, "Error retrieving account info Code:${rs.statusCode().value()} Msg:$it")
                        }
                }
                return@exchangeToMono rs.bodyToMono(String::class.java)
                    .mapNotNull { string ->
                        val user = mappingHelper.mapOne(string, User::class.java)
                        return@mapNotNull user
                    }.switchIfEmpty(Mono.error(ApiException(500000, "Error retrieving API account info. Empty body")))
            }
    }
}
