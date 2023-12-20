package com.alcosi.nft.apigateway.config.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@ConditionalOnClass(ObjectMapper::class)
@ConditionalOnProperty(
    prefix = "common-lib.object-mapper",
    name = ["disabled"],
    matchIfMissing = true,
    havingValue = "false",
)
@Configuration
class SerializationConfig() {
    @Bean
    fun getHttpSerializerModule(): SimpleModule {
        val module = SimpleModule()
        module.addDeserializer(HttpMethod::class.java, HttpMethodDeSerializer())
        module.addSerializer(HttpMethod::class.java, HttpMethodSerializer())
        return module
    }
}
