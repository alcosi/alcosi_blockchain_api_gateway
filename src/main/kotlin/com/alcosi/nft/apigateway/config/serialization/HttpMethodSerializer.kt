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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.http.HttpMethod

/**
 * This class is responsible for serializing a HttpMethod object into a JSON string.
 * It extends the JsonSerializer class from the Jackson library.
 */
open class HttpMethodSerializer : JsonSerializer<HttpMethod?>() {
    /**
     * Serializes a HttpMethod object into a JSON string.
     *
     * @param value the HttpMethod object to be serialized
     * @param jsonGenerator the JsonGenerator used to write JSON content
     * @param serializerProvider the SerializerProvider used for obtaining serializers for serializing nested objects
     */
    override fun serialize(
        value: HttpMethod?,
        jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider?,
    ) {
        if (value != null) {
            jsonGenerator.writeString(value.toString())
        } else {
            jsonGenerator.writeNull()
        }
    }
}
