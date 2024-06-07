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

package com.alcosi.nft.apigateway.config.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

/**
 * This class provides configuration for serialization and deserialization of objects using Jackson ObjectMapper.
 * It is a Spring configuration class annotated with @Configuration that creates and configures the necessary modules for serialization and deserialization.
 * This class is conditionally enabled based on the presence of the ObjectMapper class and configuration properties in the application.
 */
@ConditionalOnClass(ObjectMapper::class)
@ConditionalOnProperty(
    prefix = "common-lib.object-mapper",
    name = ["disabled"],
    matchIfMissing = true,
    havingValue = "false",
)
@Configuration
class SerializationConfig() {
    /**
     * Retrieves the HTTP serializer module used for serialization and deserialization of HttpMethod objects.
     *
     * @return The SimpleModule instance containing the deserializer and serializer for HttpMethod objects.
     */
    @Bean
    fun getHttpSerializerModule(): SimpleModule {
        val module = SimpleModule()
        module.addDeserializer(HttpMethod::class.java, HttpMethodDeSerializer())
        module.addSerializer(HttpMethod::class.java, HttpMethodSerializer())
        return module
    }
}
