package com.alcosi.nft.apigateway.config.db.redis

import com.alcosi.lib.executors.SchedulerTimer
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import reactor.core.publisher.Mono

@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(RedisCheckProperties::class)
@Import(value = [RedisAutoConfiguration::class, RedisReactiveAutoConfiguration::class ])
class RedisConfig : Logging {
    @Bean
    @ConditionalOnProperty(prefix = "spring.data.redis", name = ["check"], matchIfMissing = true, havingValue = "false")
    fun getRedisCheckScheduler(
        props: RedisCheckProperties,
        redisTemplate: ReactiveStringRedisTemplate,
    ): SchedulerTimer {
        val scheduler =
            object : SchedulerTimer(props.delay, "Redis-Check") {
                override fun startBatch() {
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
        return scheduler
    }
}
