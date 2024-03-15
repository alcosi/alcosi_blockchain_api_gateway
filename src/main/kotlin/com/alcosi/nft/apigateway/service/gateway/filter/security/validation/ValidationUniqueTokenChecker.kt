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

import com.alcosi.lib.executors.SchedulerTimer
import com.alcosi.lib.synchronisation.SynchronizationService
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.LocalDateTime
import java.util.logging.Level

open class ValidationUniqueTokenChecker(val synchronizationService: SynchronizationService) {
    protected open val tokenMap: MutableMap<Int, LocalDateTime> = mutableMapOf()
    protected open val scheduler =
        object : SchedulerTimer(Duration.ofSeconds(1), "SchedulerUniqueToken", Level.FINE) {
            override fun startBatch() {
                val currTime = LocalDateTime.now()
                val expired = tokenMap.filter { it.value.isBefore(currTime) }.keys
                expired.forEach { tokenMap.remove(it) }
                if (expired.size > 0) {
                    logger.info("ValidationUniqueTokenChecker cleared ${expired.size} tokens")
                }
            }
        }

    open fun isNotUnique(
        token: String,
        ttl: Long,
    ): Mono<Boolean> {
        return Mono.fromCallable { token.hashCode() }
            .subscribeOn(Schedulers.boundedElastic())
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
