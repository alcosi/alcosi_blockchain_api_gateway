package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.alcosi.lib.executors.SchedulerTimer
import com.alcosi.lib.utils.PrepareHexService
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.NullNode
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.math.BigInteger
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference


open class Oath2AuthComponent(
    val webClient: WebClient,
    val idServerUri: String,
    val clientId: String,
    val clientSecret: String,
    val scopes: List<String>,
    val grantType: String = "client_credentials",
    val username: String?,
    val password: String?,
    relativePath:String = "/connect/token"

) {
    protected open val tokenUri = "${idServerUri}${relativePath}"
    protected open val token = AtomicReference<Token>(Token("", LocalDateTime.MIN, 0, listOf()))

    @JvmRecord
    data class TokenRs @JsonCreator constructor(
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

    open class Token(
        val accessToken: String,
        creationTime: LocalDateTime,
        val expiresIn: Int,
        val scopes: List<String>,
    ) {
        val validTill = creationTime.plusSeconds(expiresIn.toLong())
    }

    protected open val scheduler = object : SchedulerTimer(Duration.ofSeconds(1)) {
        override fun startBatch() {
            val tk = token.get()
            val expireDelay = tk.expiresIn / 2
            val isCloseToExpire = tk.validTill.minusSeconds(expireDelay.toLong()).isBefore(LocalDateTime.now())
            if (isCloseToExpire) {
                val time = System.currentTimeMillis()
                val tokenFromServer = getFromServer().block()!!
                val took=System.currentTimeMillis()-time
                token.set(tokenFromServer)
                logger.info("Oath2 token taking took ${took}ms")
            }
        }
    }
    open fun getAccessToken():String{
        val tk = token.get()
        val isExpired=tk.validTill.isBefore(LocalDateTime.now())
        if (isExpired){
            throw IllegalStateException("Token is expired! ${tk.validTill}")
        }
        return token.get().accessToken
    }

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
            .subscribeOn(Schedulers.boundedElastic())

    }

   open class ListStringDeSerializer : StdDeserializer<List<String>>(List::class.java) {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): List<String> {
            if (p == null) {
                return listOf()
            }
            val jsonNode = ctxt!!.readTree(p)
            return if (jsonNode is NullNode) {
                listOf()
            } else{
                return jsonNode.textValue().split(" ")
            }
        }

    }
}