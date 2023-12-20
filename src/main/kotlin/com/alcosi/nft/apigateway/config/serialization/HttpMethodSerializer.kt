package com.alcosi.nft.apigateway.config.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.http.HttpMethod

open class HttpMethodSerializer : JsonSerializer<HttpMethod?>() {
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
