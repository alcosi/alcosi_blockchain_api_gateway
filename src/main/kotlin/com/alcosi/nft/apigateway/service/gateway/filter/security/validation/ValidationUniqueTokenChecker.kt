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
    protected open val scheduler = object : SchedulerTimer(Duration.ofSeconds(1), "SchedulerUniqueToken",Level.FINE) {
        override fun startBatch() {
            val currTime = LocalDateTime.now()
            val expired = tokenMap.filter { it.value.isBefore(currTime) }.keys
            expired.forEach { tokenMap.remove(it) }
            if (expired.size>0) {
                logger.info("ValidationUniqueTokenChecker cleared ${expired.size} tokens")
            }
        }
    }

    open fun isNotUnique(token: String, ttl: Long): Mono<Boolean> {
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