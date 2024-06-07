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
/**
 * Should use protected DefaultParts from org.springframework.http.codec.multipart package, so we use this package
 */
package org.springframework.http.codec.multipart

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import reactor.core.publisher.Flux

/**
 * The `EncryptedDefaultParts` class provides a utility for creating encrypted parts for multipart requests.
 *
 * It contains a nested class `EncryptedPart` that delegates all `Part` operations to the provided `delegate`.
 * The `EncryptedPart` class is `open`, allowing for further extension.
 *
 * The `create` function is used to create an encrypted part. It takes the `headers` and `dataBuffers` as parameters and
 * returns a new `Part` object. The `IS_JSON` header is set to "true" in the provided `headers`.
 *
 */
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
