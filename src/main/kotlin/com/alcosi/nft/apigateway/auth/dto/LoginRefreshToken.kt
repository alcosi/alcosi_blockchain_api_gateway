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

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.redis.core.RedisHash
import java.io.Serializable
import java.time.LocalDateTime
import java.util.*

/**
 * This class represents a login refresh token.
 *
 * @property refreshToken The refresh token.
 * @property jwtHash The JWT hash.
 * @property updatedAt The time when the token was last updated. Defaults to the current date and time.
 */
@RedisHash("LoginRefreshToken")
data class LoginRefreshToken(
    val refreshToken: UUID,
    val jwtHash: Int,
    @JsonFormat(shape = JsonFormat.Shape.STRING)val updatedAt: LocalDateTime = LocalDateTime.now(),
) : Serializable
