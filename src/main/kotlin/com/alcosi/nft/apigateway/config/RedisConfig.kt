package com.alcosi.nft.apigateway.config

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
@Configuration
@Import(value=[RedisAutoConfiguration::class, RedisReactiveAutoConfiguration::class ])
class RedisConfig:Logging{
    @Bean
    @ConditionalOnProperty(prefix = "check.redis_status", name = ["disabled"], matchIfMissing = true, havingValue = "false")
    fun getRedisCheckScheduler(
        @Value("\${check.redis_status.delay:5s}") scheduleDelay: Duration,
        redisTemplate: ReactiveStringRedisTemplate
    ): Timer {
        val task = object : TimerTask() {
            override fun run() {
                val connection = redisTemplate.connectionFactory.reactiveConnection
                val resultMono = connection.ping()
                resultMono.map {
                    if ("PONG".equals(it, ignoreCase = true)) {
                        logger.info("Redis status: connected")
                        return@map Mono.empty<Void>()
                    } else {
                        logger.info("Redis status: connection problem")
                        throw IllegalStateException("Redis connection problem!")
                    }
                }.subscribe()
            }
        }
        val timer = Timer()
        timer.scheduleAtFixedRate(task, scheduleDelay.toMillis(), scheduleDelay.toMillis())
        return timer
    }
}