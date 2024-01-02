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

import com.alcosi.lib.logging.annotations.LogTime
import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.auth.dto.LoginRefreshToken
import com.alcosi.nft.apigateway.service.exception.auth.NotValidRTException
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Duration
import java.time.LocalDateTime
import java.util.*


private const val KEY_PREFIX = "REFRESH_TOKEN"

open class RefreshTokenDBComponent(
    redisTemplate: ReactiveStringRedisTemplate,
    val mappingHelper: MappingHelper,
     val rtLifetime: Duration
) : Logging {
    val redisOpsForValue = redisTemplate.opsForValue();

    open fun checkIsValid(rt: UUID, jwtHash: Int, wallet: String): Mono<Boolean> {
        return redisOpsForValue.get(getRedisId(wallet))
            .switchIfEmpty {
                logger.error("Error with RT: Rt is not saved")
                throw NotValidRTException()
            }
            .map {
                try {
                    val token = mappingHelper.mapOne(it, LoginRefreshToken::class.java)!!
                    check(jwtHash == token.jwtHash)
                    { "Jwt hash is different! $jwtHash|${token.jwtHash}" }
                    check(LocalDateTime.now().isBefore(token.updatedAt.plus(rtLifetime)))
                    { "RT is outdated ${token.updatedAt} lifetime $rtLifetime" }
                    check(rt == token.refreshToken) { "Wrong rt! $rt| ${token.refreshToken}" }
                    return@map true
                } catch (t: Throwable) {
                    logger.error("Error with RT",t)
                    throw NotValidRTException()
                }
            }
    }


    @LogTime
    open fun saveNew(wallet: String, rt: UUID, jwtHash: Int): Mono<Boolean> {
        return redisOpsForValue.set(getRedisId(wallet), mappingHelper.serialize(LoginRefreshToken(
                rt,
                jwtHash
            )
        )!!)
            .map { true }
    }

    protected open fun getRedisId(wallet: String) = "${KEY_PREFIX}_$wallet"
}