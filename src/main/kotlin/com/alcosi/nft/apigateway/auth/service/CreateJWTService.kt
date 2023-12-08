/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.alcosi.nft.apigateway.auth.service

import com.alcosi.nft.apigateway.service.multi_wallet.MultiWalletProvider
import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import reactor.core.publisher.Mono
import java.security.Key
import java.time.Duration
import java.time.Instant
import java.util.*

class CreateJWTService(
    val tokenLifetime: Duration,
     val tokenIssuer: String,
     appPrivateKey: String,
    protected val multiWalletProvider: MultiWalletProvider
) {
    protected val privateKey: Key =Keys.hmacShaKeyFor(Decoders.BASE64.decode(appPrivateKey))
    protected val builder: JwtBuilder = Jwts.builder()


    fun createJWT(wallet: String?): Mono<String> {
        return multiWalletProvider
            .getWalletsListByWallet(wallet!!)
            .map {profileWalletsConfig->
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
                            tokenLifetime.toSeconds()
                        )
                    )
                )
                .signWith(privateKey)
                .compact()
        }
    }
}