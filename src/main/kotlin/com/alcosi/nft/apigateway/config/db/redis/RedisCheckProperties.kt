package com.alcosi.nft.apigateway.config.db.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "spring.data.redis.check")
data class RedisCheckProperties(var delay: Duration = Duration.ofSeconds(5), var disabled:Boolean = false)