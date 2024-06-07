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
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

open class IdentityOath2GetUserInfoComponent(
    protected val mappingHelper: ObjectMapper,
    protected val webClient: WebClient,
    protected val objectMapper: ObjectMapper,
    idServerUri: String,
    relativePath: String = "/connect/userinfo",
) {
    protected open val getUserInfoUri = "${idServerUri}$relativePath"

    open class Account
        @JsonCreator
        constructor(
            @JsonProperty("sub") open val id: String,
            @JsonProperty("preferred_username") open val preferredUsername: String?,
            @JsonProperty("name") open val name: String?,
            @JsonProperty("given_name") open val firstName: String?,
            @JsonProperty("family_name") open val lastName: String?,
        )

    //@JvmRecord
    data class Result(val response: Account?, val error: Error?) {
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
                    return@exchangeToMono rs.bodyToMono(String::class.java).defaultIfEmpty("")
                        .map { str ->
                            val statusCode = rs.statusCode().value()
                            val serverStatusCode = if (statusCode > 400 && statusCode < 404) "${statusCode}010".toLong() else 500000
                            throw ApiException(
                                serverStatusCode,
                                "Error retrieving account info Code:$statusCode Msg:$str",
                            )
                        }
                }
                rs.bodyToMono(String::class.java)
                    .mapNotNull { string ->
                        val account = mappingHelper.mapOne(string, Account::class.java)
                        return@mapNotNull account
                    }
                    .map { acc -> Result(acc, null) }
            }.switchIfEmpty(Mono.error(ApiException(500000, "Error retrieving account info. Empty body")))
    }
}
