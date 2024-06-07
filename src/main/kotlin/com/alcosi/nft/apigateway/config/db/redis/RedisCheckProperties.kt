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

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for Redis check.
 *
 * @property delay The delay duration between each check. Default is 5 seconds.
 * @property disabled A flag indicating whether the Redis check is disabled. Default is false.
 * @property check A flag indicating whether the Redis check is enabled. Default is true.
 */
@ConfigurationProperties(prefix = "spring.data.redis.check")
class RedisCheckProperties {
    /**
     * Represents the delay duration between each check in the Redis check configuration.
     *
     * The default value for this variable is 5 seconds.
     *
     * @see RedisCheckProperties
     * @see RedisConfig
     * @see getRedisCheckScheduler
     */
    var delay: Duration = Duration.ofSeconds(5)

    /**
     * Indicates whether the Redis check is disabled or not.
     *
     * The default value of this variable is `false`.
     *
     * @see RedisCheckProperties
     * @see RedisConfig
     * @see getRedisCheckScheduler
     */
    var disabled: Boolean = false

    /**
     * Represents a boolean flag indicating whether the Redis check is enabled or not.
     *
     * The default value for this variable is `true`.
     *
     * This variable is used in the RedisCheckProperties class to determine whether the Redis check is enabled or disabled.
     * It is also referenced in the RedisConfig class and the getRedisCheckScheduler function.
     *
     * @see RedisCheckProperties
     * @see RedisConfig
     * @see getRedisCheckScheduler
     */
    var check: Boolean = true
}
