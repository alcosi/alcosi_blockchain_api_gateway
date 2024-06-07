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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation

import io.github.breninsul.javatimerscheduler.registry.SchedulerType
import io.github.breninsul.javatimerscheduler.registry.TaskSchedulerRegistry
import io.github.breninsul.synchronizationstarter.service.SynchronizationService
import org.apache.logging.log4j.kotlin.Logging
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDateTime
import java.util.logging.Level

/**
 * The ValidationUniqueTokenChecker class is responsible for checking
 * the uniqueness of tokens. It uses a synchronization service for token
 * synchronization and a token map to manage tokens and their expiration.
 *
 * @property synchronizationService The synchronization service used for
 *     token synchronization.
 * @property uniqueTokenSchedulerDelay The delay between clearing expired
 *     tokens from the token map.
 * @property tokenMap The map that stores tokens and their expiration
 *     times.
 */
open class ValidationUniqueTokenChecker(
    val synchronizationService: SynchronizationService,
    val uniqueTokenSchedulerDelay: Duration = Duration.ofSeconds(1)
) : Logging {
    /**
     * The `tokenMap` property is a protected open mutable map that stores
     * integer keys and LocalDateTime values. It is used within the
     * `ValidationUniqueTokenChecker` class to manage tokens and their
     * expiration.
     *
     * @see ValidationUniqueTokenChecker
     */
    protected open val tokenMap: MutableMap<Int, LocalDateTime> = mutableMapOf()

    /**
     * Initialization block for `ValidationUniqueTokenChecker` class. This
     * section is responsible for setting up a scheduler task that periodically
     * clears the expired tokens from the `tokenMap`. /
     */
    init {
        TaskSchedulerRegistry.registerTypeTask(SchedulerType.VIRTUAL_WAIT, "SchedulerUniqueToken", uniqueTokenSchedulerDelay, loggingLevel = Level.FINE, runnable = Runnable {
            val currTime = LocalDateTime.now()
            val expired = tokenMap.filter { it.value.isBefore(currTime) }.keys
            expired.forEach { tokenMap.remove(it) }
            if (expired.size > 0) {
                logger.info("ValidationUniqueTokenChecker cleared ${expired.size} tokens")
            }
        })
    }

    /**
     * Checks if the token is unique based on its hash code.
     *
     * @param token The token to check for uniqueness.
     * @param ttl The time-to-live value in seconds for the token expiration.
     * @return A Mono emitting a boolean value indicating if the token is not unique.
     */
    open fun isNotUnique(
        token: String,
        ttl: Long,
    ): Mono<Boolean> {
        return Mono.fromCallable { token.hashCode() }
            .subscribeOn(com.alcosi.nft.apigateway.config.VirtualWebFluxScheduler)
            .map { hashCode ->
                val syncId = "VALIDATION_UNIQUE_$hashCode"
                synchronizationService.before(syncId)
                try {
                    val expDate = tokenMap[hashCode] ?: LocalDateTime.MAX
                    tokenMap[hashCode] = LocalDateTime.now().plusSeconds(ttl)
                    return@map expDate.isAfter(LocalDateTime.now())
                } finally {
                    synchronizationService.after(syncId)
                }
            }
    }
}
