package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

open class Oath2APIGetUserInfoComponent(
    protected val webClient: WebClient,
    protected val oath2AuthComponent: Oath2AuthComponent,
    idServerUri: String,
    relativePath: String = "/api/user/{id}"
) {
    open protected val getUserInfoUri = "${idServerUri}${relativePath}"

    @JvmRecord
    data class User @JsonCreator constructor(
        @JsonProperty("id")
        val id: String,
        @JsonProperty("fullName")
        val fullName: FullName,
        @JsonProperty("email")
        val email: String?,
        @JsonProperty("phoneNumber")
        val phoneNumber: String?,
        val claims: List<Claim>,
        val photo: String?
    ) {
        @JvmRecord
        data class FullName @JsonCreator constructor(
            @JsonProperty("firstName")
            val firstName: String,
            @JsonProperty("lastName")
            val lastName: String,
            @JsonProperty("JsonProperty")
            val middleName: String
        )

        @JvmRecord
        data class Claim @JsonCreator constructor(
            @JsonProperty("type") val type: String,
            @JsonProperty("value") val value: String
        )
    }

    fun getInfo(id: String): Mono<User> {
        return webClient
            .post()
            .uri(getUserInfoUri.replace("{id}",id))
            .header("Authorization", "Bearer ${oath2AuthComponent.getAccessToken()}")
            .exchangeToMono { rs ->
                if (!rs.statusCode().is2xxSuccessful) {
                   return@exchangeToMono rs.bodyToMono(String::class.java).map {
                        throw ApiException(500000,"Error retrieving account info Code:${rs.statusCode().value()} Msg:${it}")
                    }
                }
                rs.bodyToMono(User::class.java)
            }

    }
}