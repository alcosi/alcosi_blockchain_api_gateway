package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.iosDeviceCheck

import io.jsonwebtoken.Jwts
import org.apache.commons.codec.binary.Base64
import org.apache.logging.log4j.kotlin.Logging
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicReference

open class IOSDeviceCheckJWTComponent(
    val appleJWTAudience: String = "https://appleid.apple.com",
    val ttl: Long,
    val appleJWTKeyId: String,
    val appleJWTIssuer: String,
    val appleJWTSubject: String,
    privateKeyString: String,
) : Logging {
    @JvmRecord
    data class JWT(val value: String, val expiration: LocalDateTime)

    protected open val privateKey: Key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyString)))
    val atomicJWT: AtomicReference<JWT> = AtomicReference<JWT>(JWT("", LocalDateTime.MIN))

    open fun getJWTString(): String {
        val jwt = atomicJWT.get()
        return if (jwt.expiration.isAfter(LocalDateTime.now())) {
            jwt.value
        } else {
            getJWTSynchronized().value
        }
    }

    @Synchronized
    protected open fun getJWTSynchronized(): JWT {
        val existingJwt = atomicJWT.get()
        // check once again in sync mode
        if (existingJwt.expiration.isAfter(LocalDateTime.now())) {
            return existingJwt
        } else {
            val newJwt = createNewJWT()
            atomicJWT.set(newJwt)
            return newJwt
        }
    }

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
