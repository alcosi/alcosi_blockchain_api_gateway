/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
