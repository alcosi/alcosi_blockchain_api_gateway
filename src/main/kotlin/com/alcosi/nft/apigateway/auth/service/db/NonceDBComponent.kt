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

package com.alcosi.nft.apigateway.auth.service.db

import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.lib.objectMapper.serialize
import com.alcosi.nft.apigateway.auth.dto.ClientNonce
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime

/**
 * Represents a component for managing nonces in a Redis database.
 *
 * @property redisOpsForValue The Redis operations for string values.
 * @property mappingHelper The helper class for mapping objects.
 * @property lifetime The duration for which the nonce is valid.
 * @property keyPrefix The prefix for the Redis key.
 */
open class NonceDBComponent(
    redisTemplate: ReactiveStringRedisTemplate,
    val mappingHelper: ObjectMapper,
    protected val lifetime: Duration,
    protected val keyPrefix: String,
) : Logging {
    /**
     * Represents the operations for string values in Redis.
     */
    val redisOpsForValue = redisTemplate.opsForValue()

    /**
     * Represents a client nonce retrieved from the database.
     *
     * @property nonce The nonce value.
     * @property createdAt The date and time when the nonce was created.
     * @property msg The message associated with the nonce.
     * @property wallet The wallet associated with the nonce.
     * @property validUntil The date and time until which the nonce is valid.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DBClientNonce(
        val nonce: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING) val createdAt: LocalDateTime,
        val msg: String,
        val wallet: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING) val validUntil: LocalDateTime,
    )

    /**
     * Retrieves a [ClientNonce] associated with the specified wallet.
     *
     * @param wallet The wallet for which to retrieve the nonce.
     * @return A [Mono] emitting the [ClientNonce] if found, or [Mono.empty] if not found.
     */
    open fun get(wallet: String): Mono<ClientNonce?> {
        return redisOpsForValue.getAndDelete(getRedisId(wallet))
            .mapNotNull { mappingHelper.mapOne(it, DBClientNonce::class.java) }
            .mapNotNull {
                ClientNonce(
                    it!!.nonce,
                    it.createdAt,
                    it.msg,
                    it.wallet,
                    it.validUntil,
                )
            }
    }

    /**
     * Saves a new [ClientNonce] associated with the specified wallet.
     *
     * @param wallet The wallet for which to save the nonce.
     * @param nonce The [ClientNonce] to be saved.
     * @return A [Mono] emitting [Void] upon completion.
     * @throws ApiException if an error occurs while saving the nonce.
     */
    open fun saveNew(
        wallet: String,
        nonce: ClientNonce,
    ): Mono<Void> {
        try {
            val dbNonce = DBClientNonce(nonce.nonce, nonce.createdAt, nonce.msg, nonce.wallet, nonce.validUntil)
            return redisOpsForValue.set(getRedisId(wallet), mappingHelper.serialize(dbNonce)!!, lifetime)
                .map {
                    logger.info("$it")
                    it
                }.then()
        } catch (e: Throwable) {
            logger.error("ClientNonce save error,wallet:$wallet")
            throw object : ApiException(500, "Can't save nonce") {}
        }
    }

    /**
     * Generates the Redis ID for a given wallet.
     *
     * @param wallet The wallet for which to generate the Redis ID.
     * @return The Redis ID generated for the wallet.
     */
    protected open fun getRedisId(wallet: String) = "${keyPrefix}_$wallet"
}
