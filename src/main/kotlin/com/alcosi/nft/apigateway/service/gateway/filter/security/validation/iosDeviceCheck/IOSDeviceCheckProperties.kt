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

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The `IOSDeviceCheckProperties` class represents the configuration properties for iOS device check validation.
 *
 * @property disabled Whether iOS device check validation is disabled.
 * @property alwaysPassed Whether iOS device check validation should always pass.
 * @property superTokenEnabled Whether the super user token is enabled for iOS device check validation.
 * @property superUserToken The super user token used for iOS device check validation.
 * @property uri The URI for iOS device check validation.
 * @property ttl The time-to-live for iOS device check validation in milliseconds.
 * @property type The type of iOS device check validation.
 * @property jwt The JSON Web Token (JWT) configuration for iOS device check validation.
 */
@ConfigurationProperties("validation.ios.device-check")
open class IOSDeviceCheckProperties {
    /**
     * The `disabled` variable represents whether iOS device check validation is disabled.
     */
    var disabled: Boolean = false
    /**
     * The `alwaysPassed` variable represents a boolean value indicating whether a condition is always passed.
     *
     * @property alwaysPassed The boolean value indicating whether the condition is always passed.
     */
    var alwaysPassed: Boolean = false
    /**
     * `superTokenEnabled` is a Boolean variable that indicates whether the super token feature is enabled or not.
     *
     * The default value is `false`.
     */
    var superTokenEnabled: Boolean = false
    /**
     * The `superUserToken` variable is a string that represents the token for a superuser.
     */
    var superUserToken: String = ""
    /**
     * The `uri` variable represents the uniform resource identifier (URI) for the Apple DeviceCheck API endpoint.
     */
    var uri: String = "https://api.devicecheck.apple.com/v1/validate_device_token"
    /**
     * The `ttl` variable represents the time-to-live for the JWT (JSON Web Token) in seconds.
     *
     * @property ttl The time-to-live value in seconds.
     */
    var ttl: Long = 1000L
    /**
     * Represents a variable of type `TYPE` that holds the current state of an object.
     *
     * Possible values for `TYPE`:
     * - `ONLINE`
     *
     *
     * @property type The current state of an object.
     */
    var type: TYPE = TYPE.ONLINE
    /**
     * Represents a JSON Web Token (JWT) used for authentication and authorization in iOS device check requests.
     *
     * @property audenceUri The URI of the intended recipients of the JWT.
     * @property ttl The time-to-live for the JWT in seconds.
     * @property keyId The key ID used in the header of the JWT.
     * @property issuer The principal that issued the JWT.
     * @property subject The principal that the JWT is about.
     * @property privateKey The private key used for signing the JWT.
     */
    var jwt: Jwt = Jwt()
    /**
     * Enumeration defining the different types of devices.
     */
    enum class TYPE {
        ONLINE
    }

    /**
     * Represents a JSON Web Token (JWT) used for authentication and authorization purposes.
     */
    open class Jwt {
        /**
         * The `audenceUri` variable represents the URI (Uniform Resource Identifier) of the intended recipients of a JSON Web Token (JWT).
         *
         * @property audenceUri The URI of the intended recipients of the JWT.
         */
        var audenceUri: String = "https://appleid.apple.com"
        /**
         * This variable represents the time-to-live for iOS device check validation in milliseconds.
         * The default value is 600 milliseconds.
         */
        var ttl: Long = 600L
        /**
         * Represents the key ID used in the header of a JSON Web Token (JWT).
         *
         * @property keyId The key ID string.
         */
        var keyId: String = "test"
        /**
         * The `issuer` variable represents the principal that issued the JSON Web Token (JWT) used for authentication and authorization purposes.
         *
         * @property issuer The principal that issued the JWT.
         */
        var issuer: String = "test"
        /**
         * Represents a subject variable.
         *
         * @property subject The subject of the token.
         */
        var subject: String = "test"
        /**
         * Represents a private key used for signing the JSON Web Token (JWT) in iOS device check requests.
         *
         * @property privateKey The private key string.
         */
        var privateKey: String =
            "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDXFe7tTuoZ7UEKqy8XHIXfNeleS42C7FPQS8ywrWR3TA=="
    }
}
