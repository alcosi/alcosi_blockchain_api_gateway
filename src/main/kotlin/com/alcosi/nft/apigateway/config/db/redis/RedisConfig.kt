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

package com.alcosi.nft.apigateway.config.db.redis

import io.github.breninsul.javatimerscheduler.registry.SchedulerType
import io.github.breninsul.javatimerscheduler.registry.TaskSchedulerRegistry
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

/**
 * Configuration class for Redis.
 *
 * This class is responsible for configuring Redis in the application. It
 * provides a bean for creating a Redis check scheduler. The Redis check
 * scheduler periodically checks the status of the Redis connection.
 *
 * @see Logging
 * @see RedisCheckProperties
 * @see RedisAutoConfiguration
 * @see RedisReactiveAutoConfiguration
 */
@ConditionalOnProperty(prefix = "filter.config.path.security.type", name = ["method"], havingValue = "ETH_JWT", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(RedisCheckProperties::class)
@Import(value = [RedisAutoConfiguration::class, RedisReactiveAutoConfiguration::class])
class RedisConfig : Logging {
    /**
     * Retrieves the Redis check scheduler.
     *
     * This method returns a pair consisting of the task ID and the runnable instance
     * for the Redis check scheduler. The Redis check scheduler periodically checks
     * the status of the Redis connection by pinging it.
     *
     * @param props The Redis check properties including the delay duration for each check.
     * @param redisTemplate The ReactiveStringRedisTemplate instance for connecting to Redis.
     *
     * @return A pair consisting of the task ID and a Runnable instance.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.data.redis", name = ["check"], matchIfMissing = true, havingValue = "false")
    fun getRedisCheckScheduler(
        props: RedisCheckProperties,
        redisTemplate: ReactiveStringRedisTemplate,
    ): Pair<Long,Runnable>  {
        val runnable: Runnable = Runnable {
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
        val taskId= TaskSchedulerRegistry.registerTypeTask(SchedulerType.VIRTUAL_WAIT, "Redis-Check", props.delay, runnable = runnable)
        return taskId to runnable
    }
}
