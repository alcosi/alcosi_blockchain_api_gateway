package org.springframework.http.codec.multipart

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Flux

object EncryptedDefaultParts {
    open class EncryptedPart(delegate: Part) : Part by delegate

    fun create(
        headers: HttpHeaders,
        dataBuffers: Flux<DataBuffer>,
    ): Part {
        headers["IS_JSON"] = "true"
        return EncryptedPart(DefaultParts.part(headers, dataBuffers))
    }
}
