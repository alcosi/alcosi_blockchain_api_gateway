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

package com.alcosi.nft.apigateway.auth.service

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.auth.dto.ClientNonce
import com.alcosi.nft.apigateway.auth.service.db.NonceDBComponent
import com.alcosi.nft.apigateway.service.exception.auth.NoNonceException
import kotlinx.coroutines.reactor.mono
import org.apache.logging.log4j.kotlin.Logging
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

open class NonceComponent(
    protected val prepareArgsService: PrepareHexService,
    protected val prepareMsgComponent: PrepareLoginMsgComponent,
    protected val nonceDBComponent: NonceDBComponent,
    protected val lifetime: Duration,
) : Logging {
    protected open val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
    protected open val random: Random = SecureRandom()

    open fun getNewNonce(wallet: String?): Mono<ClientNonce> {
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
                currentDate.plus(lifetime),
            )
        }
            .flatMap { nonceDBComponent.saveNew(it.wallet, it).thenReturn(it) }
    }

    open fun getSavedNonce(wallet: String): Mono<ClientNonce> {
        return (
            nonceDBComponent.get(wallet)
                .switchIfEmpty { throw NoNonceException(wallet) }
        )
            as Mono<ClientNonce>
    }

    open fun nonce(): BigInteger {
        val id = UUID.randomUUID()
        return BigInteger(
            ByteBuffer.allocate(16).putLong(id.mostSignificantBits).putLong(id.leastSignificantBits).array(),
        )
    }
}
