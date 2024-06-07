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

package com.alcosi.nft.apigateway.auth.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.apache.logging.log4j.kotlin.Logging

/**
 * CheckJWTService class is responsible for parsing and verifying JSON Web Tokens (JWT).
 *
 * @property privateKey The private key used for verification.
 */
open class CheckJWTService(privateKey: String) : Logging {
    /**
     * KDoc for `jwtParser` variable.
     *
     * This variable represents a `JwtParser` object used for parsing and verifying JSON Web Tokens (JWT).
     *
     * @property jwtParser The lazy-initialized `JwtParser` object used for parsing and verifying JSON Web Tokens (JWT).
     *                    It uses the `privateKey` provided to the `CheckJWTService` class constructor.
     *
     * @see CheckJWTService
     * @see CheckJWTService.buildParser
     * @see CheckJWTService.parse
     */
    protected open val jwtParser: JwtParser by lazy { buildParser(privateKey) }

    /**
     * Builds a JwtParser object used for parsing and verifying JSON Web Tokens (JWT).
     *
     * @param pk The base64 encoded private key used for verification.
     * @return The JwtParser object.
     */
    protected open fun buildParser(pk: String): JwtParser {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(pk)))
            .build()
    }

    /**
     * Parses the provided JSON Web Token (JWT) string and returns the Claims object.
     *
     * @param jwtString The JSON Web Token (JWT) string to be parsed.
     * @return The Claims object containing the parsed claims from the JWT.
     */
    open fun parse(jwtString: String): Claims {
        val claimsJws = jwtParser.parseSignedClaims(jwtString)
        return claimsJws.payload
    }
}
