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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.http.HttpMethod
import java.io.IOException

/**
 * This class is responsible for deserializing a JSON string into a HttpMethod object.
 * It extends the JsonDeserializer class from the Jackson library.
 *
 * @constructor Creates an instance of HttpMethodDeSerializer.
 */
open class HttpMethodDeSerializer : JsonDeserializer<HttpMethod?>() {
    /**
     * This method deserializes a JSON string into a HttpMethod object.
     *
     * @param jsonParser The JSON parser used to read the JSON string.
     * @param context The deserialization context used during deserialization.
     * @return The deserialized HttpMethod object, or null if the JSON string is null.
     * @throws IOException If an error occurs during deserialization or if the HttpMethod is unknown.
     */
    override fun deserialize(
        jsonParser: JsonParser?,
        context: DeserializationContext?,
    ): HttpMethod? {
        val methodAsString: String? = jsonParser?.text
        try {
            return methodAsString?.let { HttpMethod.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            throw IOException("Unknown HttpMethod: $methodAsString", e)
        }
    }
}
