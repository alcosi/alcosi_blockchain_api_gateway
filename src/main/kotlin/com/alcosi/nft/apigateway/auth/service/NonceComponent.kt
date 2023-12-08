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

package com.alcosi.nft.apigateway.auth.service

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.dto.ClientNonce
import com.alcosi.nft.apigateway.auth.service.db.NonceDBComponent
import com.alcosi.nft.apigateway.service.exception.auth.NoNonceException
import kotlinx.coroutines.reactor.mono
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class NonceComponent(
    protected val prepareArgsService: PrepareHexService,
    protected val prepareMsgComponent: PrepareLoginMsgComponent,
    protected val nonceDBComponent: NonceDBComponent,
    @Value("\${jwt.nonce.lifetime:5m}") protected val lifetime: Duration,
    ) : Logging {
    protected val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
    protected val random = SecureRandom();
    fun getNewNonce(wallet: String?): Mono<ClientNonce> {
        return mono {
            logger.info("Get new nonce for wallet - $wallet")
            val currentDate = LocalDateTime.now()
            val nonce = "${currentDate.format(dateTimeFormatter)}:${random.nextLong().toULong()}"
            val prepareWallet = prepareArgsService.prepareAddr(wallet)
            ClientNonce(
                nonce,
                currentDate,
                prepareMsgComponent.getMsg(prepareWallet, nonce),
                prepareWallet,
                currentDate.plus(lifetime)
            )
        }
            .flatMap { nonceDBComponent.saveNew(it.wallet, it).thenReturn(it) }
    }

    fun getSavedNonce(wallet: String): Mono<ClientNonce> {
        return (nonceDBComponent.get(wallet)
            .switchIfEmpty { throw NoNonceException(wallet)  })
                as Mono<ClientNonce>
    }

    fun nonce(): BigInteger {
        val id = UUID.randomUUID()
        return BigInteger(
            ByteBuffer.allocate(16).putLong(id.mostSignificantBits).putLong(id.leastSignificantBits).array()
        );
    }
}