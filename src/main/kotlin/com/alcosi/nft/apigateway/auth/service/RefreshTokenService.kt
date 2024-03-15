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

import com.alcosi.nft.apigateway.auth.dto.JWTAndRefreshToken
import com.alcosi.nft.apigateway.auth.service.db.RefreshTokenDBComponent
import com.alcosi.nft.apigateway.service.exception.auth.WrongWalletException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

open class RefreshTokenService(
    protected val createJWTService: CreateJWTService,
    protected val dbComponent: RefreshTokenDBComponent,
    protected val checkJWTService: CheckJWTService,
) {
    open fun refresh(
        wallet: String,
        jwtString: String,
        rt: UUID,
    ): Mono<JWTAndRefreshToken> {
        val walletFromJWTMono = mono { getWalletFromToken(jwtString) }
        val walletMono =
            walletFromJWTMono.flatMap { walletFromJWT ->
                if (wallet != walletFromJWT) {
                    return@flatMap Mono.error(WrongWalletException(wallet))
                }
                dbComponent.checkIsValid(rt, jwtString.hashCode(), wallet)
                    .zipWith(wallet.toMono())
                    .map { it.t2 }
            }
        return walletMono.flatMap { w -> saveInfo(w) }
    }

    open fun saveInfo(wallet: String): Mono<JWTAndRefreshToken> {
        val newRt = UUID.randomUUID()
        return createJWTService.createJWT(wallet).flatMap {
            dbComponent.saveNew(wallet, newRt, it.hashCode())
                .map { _ -> (JWTAndRefreshToken(newRt, it)) }
        }
    }

    protected open fun getWalletFromToken(jwtString: String): String {
        return try {
            return parseClaims(checkJWTService.parse(jwtString))
        } catch (e: ExpiredJwtException) {
            parseClaims(e.claims)
        }
    }

    protected open fun parseClaims(claims: Claims): String {
        return claims.get("currentWallet", String::class.java)
    }
}
