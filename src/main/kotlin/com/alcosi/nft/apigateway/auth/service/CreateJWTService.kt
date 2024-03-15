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

open class CreateJWTService(
    val tokenLifetime: Duration,
    val tokenIssuer: String,
    appPrivateKey: String,
    protected val multiWalletProvider: MultiWalletProvider,
) {
    protected open val privateKey: Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(appPrivateKey))
    protected open val builder: JwtBuilder = Jwts.builder()

    open fun createJWT(wallet: String?): Mono<String> {
        return multiWalletProvider
            .getWalletsListByWallet(wallet!!)
            .map { profileWalletsConfig ->
                builder
                    .setIssuer(tokenIssuer)
                    .setSubject("profile")
                    .claim("currentWallet", wallet)
                    .claim("profileId", profileWalletsConfig.profileId)
                    .claim("profileWallets", profileWalletsConfig.wallets)
                    .setId(wallet)
                    .claim("authorities", listOf("ALL"))
                    .setIssuedAt(Date.from(Instant.now()))
                    .setExpiration(
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
