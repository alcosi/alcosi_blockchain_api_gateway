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
import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.auth.dto.LoginRefreshToken
import com.alcosi.nft.apigateway.service.exception.auth.NotValidRTException
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

private const val KEY_PREFIX = "REFRESH_TOKEN"

open class RefreshTokenDBComponent(
    redisTemplate: ReactiveStringRedisTemplate,
    val mappingHelper: MappingHelper,
    val rtLifetime: Duration,
) : Logging {
    val redisOpsForValue = redisTemplate.opsForValue()

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

    protected open fun getRedisId(wallet: String) = "${KEY_PREFIX}_$wallet"
}
