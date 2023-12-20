package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.Oath2AuthComponent
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

open class IdentityOath2APIGetUserInfoComponent(
    protected val mappingHelper: MappingHelper,
    protected val webClient: WebClient,
    protected val oath2AuthComponent: Oath2AuthComponent,
    protected val apiVersion: String,
    idServerUri: String,
    relativePath: String = "/api/user/{id}",
) {
    protected open val getUserInfoUri = "${idServerUri}$relativePath"

    @JvmRecord
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
            @JvmRecord
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

            @JvmRecord
            data class Claim
                @JsonCreator
                constructor(
                    @JsonProperty("type") val type: String,
                    @JsonProperty("value") val value: String,
                )
        }

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
