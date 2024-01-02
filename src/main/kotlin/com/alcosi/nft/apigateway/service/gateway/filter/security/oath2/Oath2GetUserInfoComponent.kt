package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

open class Oath2GetUserInfoComponent(
    protected val webClient: WebClient,
    protected val objectMapper: ObjectMapper,
    idServerUri: String,
    relativePath: String = "/connect/userinfo"
) {
    open protected val getUserInfoUri = "${idServerUri}${relativePath}"

    open class Account @JsonCreator constructor(
        @JsonProperty("sub") open val id: String,
        @JsonProperty("preferred_username") open val preferredUsername: String?,
        @JsonProperty("name") open val name: String?,
        @JsonProperty("given_name") open val firstName: String?,
        @JsonProperty("family_name") open val lastName: String?,
    )

    @JvmRecord
    data class Result(val response: Account?, val error: Error?, private val objectMapper: ObjectMapper) {
        data class Error(val httpCode: HttpStatusCode, val body: String?)

        fun isError(): Boolean {
            return error != null
        }

    }

    fun getInfo(token: String): Mono<Result> {
        return webClient
            .post()
            .uri(getUserInfoUri)
            .header("Authorization", "Bearer $token")
            .exchangeToMono { rs ->
                if (!rs.statusCode().is2xxSuccessful) {
                    return@exchangeToMono rs.bodyToMono(String::class.java)
                        .map { str -> Result(null, Result.Error(rs.statusCode(), str), objectMapper) }
                }
                rs.bodyToMono(Account::class.java).map { acc -> Result(acc, null, objectMapper) }
            }

    }
}