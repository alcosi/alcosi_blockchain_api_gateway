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

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.auth.dto.ClientNonce
import com.alcosi.nft.apigateway.service.error.exceptions.ApiException
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime

open class NonceDBComponent(
    redisTemplate: ReactiveStringRedisTemplate,
    val mappingHelper: MappingHelper,
    protected val lifetime: Duration,
    protected val keyPrefix: String,
) : Logging {
    val redisOpsForValue = redisTemplate.opsForValue()

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JvmRecord
    data class DBClientNonce(
        val nonce: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING) val createdAt: LocalDateTime,
        val msg: String,
        val wallet: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING) val validUntil: LocalDateTime,
    )

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

    protected open fun getRedisId(wallet: String) = "${keyPrefix}_$wallet"
}
