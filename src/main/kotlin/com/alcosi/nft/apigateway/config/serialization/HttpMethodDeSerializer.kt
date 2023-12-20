package com.alcosi.nft.apigateway.config.serialization

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.http.HttpMethod
import java.io.IOException

open class HttpMethodDeSerializer : JsonDeserializer<HttpMethod?>() {
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
