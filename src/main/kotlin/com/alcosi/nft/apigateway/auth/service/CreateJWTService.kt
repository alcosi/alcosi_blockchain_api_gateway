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

import com.alcosi.nft.apigateway.service.multiWallet.MultiWalletProvider
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import reactor.core.publisher.Mono
import java.security.Key
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * CreateJWTService class is responsible for creating JSON Web Tokens (JWT).
 *
 * @property tokenLifetime The lifetime of the token.
 * @property tokenIssuer The issuer of the token.
 * @property privateKey The private key used for signing the token.
 * @property multiWalletProvider The provider for retrieving wallet information.
 */
open class CreateJWTService(
    val tokenLifetime: Duration,
    val tokenIssuer: String,
    appPrivateKey: String,
    protected val multiWalletProvider: MultiWalletProvider,
) {
    /**
     * Represents the private key used for signing JSON Web Tokens (JWT) in the `CreateJWTService` class.
     *
     * @property privateKey The private key used for signing the token.
     */
    protected open val privateKey: Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(appPrivateKey))

    /**
     * The `builder` property is responsible for building JSON Web Tokens (JWT) using the JwtBuilder class.
     *
     * It is a protected open property that is initialized with a new instance of JwtBuilder from the Jwts class.
     *
     * @property builder The JwtBuilder instance used for building JWT.
     */
    protected open val builder: JwtBuilder = Jwts.builder()

    /**
     * Creates a JSON Web Token (JWT) for the given wallet.
     *
     * @param wallet The wallet name.
     * @return A Mono emitting the JWT string.
     */
    open fun createJWT(wallet: String?): Mono<String> {
        return multiWalletProvider
            .getWalletsListByWallet(wallet!!)
            .map { profileWalletsConfig ->
                builder
                    .issuer(tokenIssuer)
                    .subject("profile")
                    .claim("currentWallet", wallet)
                    .claim("profileId", profileWalletsConfig.profileId)
                    .claim("profileWallets", profileWalletsConfig.wallets)
                    .id(wallet)
                    .claim("authorities", listOf("ALL"))
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(
                        Date.from(
                            Instant.ofEpochMilli(System.currentTimeMillis()).plusSeconds(
                                tokenLifetime.toSeconds(),
                            ),
                        ),
                    )
                    .signWith(privateKey)
                    .compact()
            }
    }
}
