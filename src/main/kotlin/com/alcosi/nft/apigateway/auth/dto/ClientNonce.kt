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

package com.alcosi.nft.apigateway.auth.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@JvmRecord
data class ClientNonce(
    val nonce: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val createdAt: LocalDateTime,
    val msg: String,
    val wallet: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @JsonAlias("time")
    val validUntil: LocalDateTime,
) {
    @JsonProperty("time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    fun time(): LocalDateTime {
        return createdAt
    }
}
