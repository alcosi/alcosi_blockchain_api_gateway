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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.iosDeviceCheck

import com.alcosi.lib.executors.sync
import io.jsonwebtoken.Jwts
import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.kotlin.Logging
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

/**
 * The `IOSDeviceCheckJWTComponent` class is responsible for generating and managing JSON Web Tokens (JWTs) for iOS device check requests.
 *
 * @param appleJWTAudience The audience claim of the JWT. It specifies the intended recipients of the JWT.
 * @param ttl The time-to-live for the JWT in seconds.
 * @param appleJWTKeyId The key ID used in the header of the JWT.
 * @param appleJWTIssuer The issuer claim of the JWT. It identifies the principal that issued the JWT.
 * @param appleJWTSubject The subject claim of the JWT. It identifies the principal that the JWT is about.
 * @param privateKeyString The private key used for signing the JWT.
 */
open class IOSDeviceCheckJWTComponent(
    val appleJWTAudience: String = "https://appleid.apple.com",
    val ttl: Long,
    val appleJWTKeyId: String,
    val appleJWTIssuer: String,
    val appleJWTSubject: String,
    privateKeyString: String,
) : Logging {
    /**
     * Represents a JWT (JSON Web Token).
     *
     * @property value The token value.
     * @property expiration The expiration date and time of the token.
     */
    data class JWT(val value: String, val expiration: LocalDateTime)

    /**
     * The private key used for generating JSON Web Tokens (JWTs).
     *
     * @property privateKey The private key object.
     */
    protected open val privateKey: Key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyString)))

    /**
     * A thread-safe variable holding an AtomicReference to a JWT object.
     *
     * The `atomicJWT` variable is used to store and update a JWT (JSON Web Token) object in a thread-safe manner.
     * It utilizes the `AtomicReference` class to ensure atomic operations on the JWT object.
     *
     * @property atomicJWT The AtomicReference to the JWT object.
     */
    protected open val atomicJWT: AtomicReference<JWT> = AtomicReference<JWT>(JWT("", LocalDateTime.MIN))

    /**
     * Represents a semaphore used for controlling access to a critical section of code.
     *
     * @property jwtSemaphore The semaphore object.
     */
    protected open val jwtSemaphore: Semaphore = Semaphore(1)
    /**
     * Retrieves the JSON Web Token (JWT) string.
     *
     * @return The JWT string.
     */
    open fun getJWTString(): String {
        val jwt = atomicJWT.get()
        return if (jwt.expiration.isAfter(LocalDateTime.now())) {
            jwt.value
        } else {
            getJWTSynchronized().value
        }
    }

    /**
     * Gets the JWT (JSON Web Token) in a synchronized manner.
     *
     * @return The JWT object.
     */
    protected open fun getJWTSynchronized(): JWT {
        return jwtSemaphore.sync {
            val existingJwt = atomicJWT.get()
            // check once again in sync mode
            if (existingJwt.expiration.isAfter(LocalDateTime.now())) {
                return@sync existingJwt
            } else {
                val newJwt = createNewJWT()
                atomicJWT.set(newJwt)
                return@sync  newJwt
            }
        }
    }

    /**
     * Creates a new JSON Web Token (JWT) object.
     *
     * @return The newly created JWT object.
     */
    protected open fun createNewJWT(): JWT {
        logger.trace("Apple JWT is out of date. Creating new")
        val now = LocalDateTime.now()
        val expiration = now.plusSeconds(ttl)
        val builder = Jwts.builder()
        builder.header().keyId(appleJWTKeyId)
        builder.issuer(appleJWTIssuer)
        builder.issuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()))
        builder.expiration(Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant()))
        builder.audience().add(appleJWTAudience)
        builder.subject(appleJWTSubject)
        builder.signWith(privateKey)
        val jwtString = builder.compact()
        return JWT(jwtString, expiration)
    }
}
