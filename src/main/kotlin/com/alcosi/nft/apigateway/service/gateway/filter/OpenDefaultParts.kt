package org.springframework.http.codec.multipart

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Flux

object OpenDefaultParts {
    fun create(headers: HttpHeaders, dataBuffers: Flux<DataBuffer>):Part{
       return DefaultParts.part(headers,dataBuffers)
    }
}