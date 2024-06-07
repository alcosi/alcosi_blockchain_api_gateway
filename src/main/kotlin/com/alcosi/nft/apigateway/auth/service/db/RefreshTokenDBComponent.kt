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

import com.alcosi.lib.logging.annotations.LogTime
import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.lib.objectMapper.serialize
import com.alcosi.nft.apigateway.auth.dto.LoginRefreshToken
import com.alcosi.nft.apigateway.service.exception.auth.NotValidRTException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

/**
 * Prefix used for keys related to refresh tokens.
 *
 * The value of this constant is "REFRESH_TOKEN".
 */
private const val KEY_PREFIX = "REFRESH_TOKEN"

/**
 * This class represents a component for managing refresh tokens in a Redis database.
 *
 * @property redisTemplate The Redis template used for interacting with the Redis server.
 * @property mappingHelper The mapping helper used for object serialization and deserialization.
 * @property rtLifetime The lifetime duration of the refresh token.
 */
open class RefreshTokenDBComponent(
    redisTemplate: ReactiveStringRedisTemplate,
    val mappingHelper: ObjectMapper,
    val rtLifetime: Duration,
) : Logging {
    /**
     * Variable representing the Redis operations for value in the RedisTemplate.
     *
     * This variable provides access to Redis operations for values. It can be used to perform operations
     * such as getting, setting, and deleting values in Redis.
     */
    val redisOpsForValue = redisTemplate.opsForValue()

    /**
     * Checks if a given refresh token is valid.
     *
     * @param rt The refresh token.
     * @param jwtHash The hash code of the JWT.
     * @param wallet The wallet associated with the refresh token.
     * @return A Mono indicating whether the refresh token is valid (true) or not (false).
     * @throws NotValidRTException If the refresh token is not valid.
     */
    open fun checkIsValid(
        rt: UUID,
        jwtHash: Int,
        wallet: String,
    ): Mono<Boolean> {
        return redisOpsForValue.get(getRedisId(wallet))
            .switchIfEmpty {
                logger.error("Error with RT: Rt is not saved")
                throw NotValidRTException()
            }
            .map {
                try {
                    val token = mappingHelper.mapOne(it, LoginRefreshToken::class.java)!!
                    check(jwtHash == token.jwtHash) { "Jwt hash is different! $jwtHash|${token.jwtHash}" }
                    check(LocalDateTime.now().isBefore(token.updatedAt.plus(rtLifetime))) { "RT is outdated ${token.updatedAt} lifetime $rtLifetime" }
                    check(rt == token.refreshToken) { "Wrong rt! $rt| ${token.refreshToken}" }
                    return@map true
                } catch (t: Throwable) {
                    logger.error("Error with RT", t)
                    throw NotValidRTException()
                }
            }
    }

    /**
     * Saves a new LoginRefreshToken object in Redis.
     *
     * @param wallet The wallet associated with the refresh token.
     * @param rt The refresh token.
     * @param jwtHash The hash code of the JWT.
     * @return A Mono indicating whether the saving was successful (true) or not (false).
     */
    @LogTime
    open fun saveNew(
        wallet: String,
        rt: UUID,
        jwtHash: Int,
    ): Mono<Boolean> {
        return redisOpsForValue.set(
            getRedisId(wallet),
            mappingHelper.serialize(
                LoginRefreshToken(
                    rt,
                    jwtHash,
                ),
            )!!,
        )
            .map { true }
    }

    /**
     * Returns the Redis ID for the given wallet.
     *
     * @param wallet The wallet name.
     * @return The Redis ID for the wallet.
     */
    protected open fun getRedisId(wallet: String) = "${KEY_PREFIX}_$wallet"
}
