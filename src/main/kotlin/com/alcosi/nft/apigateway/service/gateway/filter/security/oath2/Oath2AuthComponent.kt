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

package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.NullNode
import io.github.breninsul.javatimerscheduler.registry.SchedulerType
import io.github.breninsul.javatimerscheduler.registry.TaskSchedulerRegistry
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference


/**
 * Oath2AuthComponent is a class responsible for handling OAuth2
 * authentication and authorization.
 *
 * @property webClient The WebClient instance used for making HTTP
 *     requests.
 * @property idServerUri The URI of the identity server.
 * @property clientId The client ID used for authentication.
 * @property clientSecret The client secret used for authentication.
 * @property scopes The list of scopes associated with the authentication.
 * @property grantType The grant type for authentication. Default is
 *     "client_credentials".
 * @property username The username for authentication. If not provided,
 *     username/password authentication is not used.
 * @property password The password for authentication. Required only if
 *     username is provided.
 * @property relativePath The relative path for retrieving tokens. Default
 *     is "/connect/token"
 * @property checkRefreshMachineTokenDelay Delay for token update check
 */
open class Oath2AuthComponent(
    val webClient: WebClient,
    val idServerUri: String,
    val clientId: String,
    val clientSecret: String,
    val scopes: List<String>,
    val grantType: String = "client_credentials",
    val username: String?,
    val password: String?,
    relativePath: String = "/connect/token",
    val checkRefreshMachineTokenDelay: Duration = Duration.ofSeconds(1)
) : Logging {
    /**
     * Represents the URI for retrieving tokens.
     *
     * The `tokenUri` property is a concatenation of the `idServerUri` and
     * `relativePath` properties.
     *
     * @property tokenUri The URI for retrieving tokens.
     */
    protected open val tokenUri = "${idServerUri}$relativePath"

    /**
     * A protected open property representing an atomic reference to a Token
     * object.
     *
     * The Token class represents an access token used for authentication. It
     * contains the following properties:
     * - accessToken: The access token string.
     * - expiresIn: The duration in seconds until the token expires.
     * - scopes: A list of scope strings associated with the token.
     * - validTill: The LocalDateTime representing the expiration datetime of
     *   the token.
     *
     * @constructor Creates an AtomicReference object with an initial Token
     *     instance.
     * @property token The AtomicReference object holding the Token instance.
     */
    protected open val token = AtomicReference<Token>(Token("", LocalDateTime.MIN, 0, listOf()))

    /**
     * Represents a token response obtained from the server.
     *
     * @property accessToken The access token.
     * @property tokenType The type of token.
     * @property expiresIn The time in seconds until the token expires.
     * @property scopes The list of scopes associated with the token.
     */
    data class TokenRs
    @JsonCreator
    constructor(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("expires_in")
        val expiresIn: Int,
        @JsonProperty("scope")
        @JsonDeserialize(using = ListStringDeSerializer::class)
        val scopes: List<String>,
    )

    /**
     * Represents a Token that is used for authentication and authorization.
     *
     * @property accessToken The access token.
     * @property creationTime The time when the token was created.
     * @property expiresIn The time in seconds until the token expires.
     * @property scopes The list of scopes associated with the token.
     * @property validTill The time when the token becomes invalid.
     */
    open class Token(
        val accessToken: String,
        creationTime: LocalDateTime,
        val expiresIn: Int,
        val scopes: List<String>,
    ) {
        val validTill = creationTime.plusSeconds(expiresIn.toLong())
    }

    /**
     * Initialization block of the Oath2AuthComponent class. This block
     * is called when an instance of the class is created. It registers a
     * task with the TaskSchedulerRegistry that periodically checks if the
     * OAuth2 token is about to expire and if so, fetches a new one from the
     * server. The task is scheduled to run at a frequency defined by the
     * checkRefreshMachineTokenDelay.
     */
    init {
        TaskSchedulerRegistry.registerTypeTask(SchedulerType.VIRTUAL_WAIT, "UpdateOath2Machine2MachineToken", checkRefreshMachineTokenDelay, runnable = Runnable {
            val tk = token.get()
            val expireDelay = tk.expiresIn / 2
            val isCloseToExpire = tk.validTill.minusSeconds(expireDelay.toLong()).isBefore(LocalDateTime.now())
            if (isCloseToExpire) {
                val time = System.currentTimeMillis()
                val tokenFromServer = getFromServer().block()!!
                val took = System.currentTimeMillis() - time
                token.set(tokenFromServer)
                logger.info("Oath2 token taking took ${took}ms")
            }
        })

    }

    /**
     * Retrieves the access token.
     *
     * @return The access token.
     * @throws IllegalStateException if the token is expired.
     */
    open fun getAccessToken(): String {
        val tk = token.get()
        val isExpired = tk.validTill.isBefore(LocalDateTime.now())
        if (isExpired) {
            throw IllegalStateException("Token is expired! ${tk.validTill}")
        }
        return token.get().accessToken
    }

    /**
     * Retrieves a token from the server.
     *
     * @return A Mono that emits a Token object representing the retrieved token.
     * @throws IllegalStateException if the HTTP response is not successful.
     */
    open fun getFromServer(): Mono<Token> {
        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        listOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "client_secret" to clientSecret,
            "scope" to scopes.joinToString(" "),
            "grant_type" to grantType,
            "username" to username,
            "password" to password,
        ).filter { it.second != null }
            .forEach { p -> formData.add(p.first, p.second) }
        val rqTime = LocalDateTime.now()
        return webClient
            .post()
            .uri(tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .exchangeToMono { r ->
                if (r.statusCode().isError) {
                    throw IllegalStateException("Http rs is not successfully ${r.statusCode()} ")
                }
                r.bodyToMono(TokenRs::class.java)
                    .map { Token(it.accessToken, rqTime, it.expiresIn, it.scopes) }
            }
            .subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
    }

    /**
     * ListStringDeSerializer is a custom deserializer for deserializing JSON strings into lists of strings.
     *
     * This class extends the StdDeserializer class and overrides the deserialize method to provide
     * custom deserialization logic.
     */
    open class ListStringDeSerializer : StdDeserializer<List<String>>(List::class.java) {
        override fun deserialize(
            p: JsonParser?,
            ctxt: DeserializationContext?,
        ): List<String> {
            if (p == null) {
                return listOf()
            }
            val jsonNode = ctxt!!.readTree(p)
            return if (jsonNode is NullNode) {
                listOf()
            } else {
                return jsonNode.textValue().split(" ")
            }
        }
    }
}
