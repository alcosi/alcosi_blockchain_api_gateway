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

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * The `defaultLoginTemplate` variable is a private constant that represents the default login template.
 *
 * This template is used to display a message to the user when they need to connect their wallet.
 * It contains a placeholder, `@nonce@`, which will be replaced with the actual nonce value.
 *
 */
private const val defaultLoginTemplate = """
Please connect your wallet
@nonce@
"""

/**
 * Configuration properties for EthJwt.
 */
@ConfigurationProperties("jwt")
open class EthJwtProperties {
    /**
     * Represents a key for configuration properties in EthJwtProperties.
     *
     * @property privateKey The private key value.
     */
    var key: Key = Key()

    /**
     * Represents the checkSignDisable property.
     *
     * This property determines whether the signature of the authentication token should be checked or not.
     * If set to true, the signature will not be checked, otherwise, it will be checked.
     */
    var checkSignDisable: Boolean = false

    /**
     * Represents a token used for authentication.
     *
     * @property issuer The issuer of the token.
     * @property lifetime The lifetime of the token.
     * @property rtLifetime The lifetime of the refresh token.
     */
    var token: Token = Token()

    /**
     * Represents a nonce used for authentication.
     *
     * @property lifetime The lifetime of the nonce.
     * @property redisPrefix The Redis prefix used for storing the nonce.
     */
    var nonce: Nonce = Nonce()

    /**
     * Represents the login template for EthJwtProperties.
     *
     * The login template is used by the PrepareLoginMsgComponent to prepare login messages.
     * It is a string that contains the message template for login.
     *
     * @see EthJwtProperties
     * @see PrepareLoginMsgComponent
     */
    var loginTemplate: String = defaultLoginTemplate

    /**
     * Represents a Nonce used for authentication.
     */
    open class Nonce {
        /**
         * Represents the lifetime of a variable.
         *
         * The lifetime represents the duration for which the variable is considered valid or active.
         *
         * @property lifetime The duration of the variable's lifetime.
         */
        var lifetime: Duration = Duration.ofMinutes(5)

        /**
         * Represents the Redis prefix used for storing the nonce in the EthJwtProperties class.
         *
         * The Redis prefix is a string that is used as a prefix when storing the nonce in the Redis server.
         * It is used to differentiate the nonce from other keys stored in the Redis server.
         *
         * @property redisPrefix The Redis prefix used for storing the nonce.
         * @see EthJwtProperties
         * @see NonceDBComponent
         * @see Nonce
         */
        var redisPrefix: String = "LOGIN_NONCE"
    }

    /**
     * Represents a token that can be issued for authentication.
     *
     * The Token class is an open class that can be subclassed to represent different types of tokens.
     * It has properties for the issuer, lifetime, and refresh token lifetime.
     * The issuer represents the entity that issued the token.
     * The lifetime represents the duration for which the token is considered valid.
     * The refresh token lifetime represents the duration for which the refresh token is considered valid.
     *
     * @property issuer The issuer of the token.
     * @property lifetime The duration of the token's lifetime.
     * @property rtLifetime The duration of the refresh token's lifetime.
     */
    open class Token {
        /**
         * Represents the issuer of the token.
         */
        var issuer: String = "Test"

        /**
         * Represents the duration of the token's lifetime.
         *
         * The lifetime represents the duration for which the token is considered valid.
         *
         * @see Token
         * @see EthJwtProperties
         */
        var lifetime: Duration = Duration.ofHours(1)

        /**
         * Represents the duration of the refresh token's lifetime.
         *
         * The refresh token lifetime represents the duration for which the refresh token is considered valid.
         *
         * @see Token
         * @see EthJwtProperties
         */
        var rtLifetime: Duration = Duration.ofDays(7)
    }

    /**
     * Represents a key used for encryption or signing.
     *
     * The Key class is an open class that can be subclassed to represent different types of keys.
     * It has a nullable `privateKey` property that can be used to store the private key value.
     *
     * @property privateKey The private key for encryption or signing.
     */
    open class Key {
        /**
         * Represents a key used for encryption or signing.
         *
         * The Key class is an open class that can be subclassed to represent different types of keys.
         * It has a nullable `privateKey` property that can be used to store the private key value.
         *
         * @property privateKey The private key for encryption or signing.
         */
        var privateKey: String? = null

        /**
         * Represents a key used for encryption or signing.
         *
         * The Key class is an open class that can be subclassed to represent different types of keys.
         * It has a nullable `privateKey` property that can be used to store the private key value.
         *
         * @property privateKey The private key for encryption or signing.
         */
        var private: String?
            get() = privateKey
            set(privateKey) {
                this.privateKey = privateKey
            }
    }
}
