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
package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.attestation

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Google Attestation validation.
 */
@ConfigurationProperties("validation.google.attestation")
open class GoogleAttestationProperties {
    /**
     * Indicates whether the feature is disabled or not.
     */
    var disabled: Boolean = false
    /**
     * Variable to indicate whether the validation for Google Attestation is always passed.
     * If set to true, the validation will always pass regardless of the actual result.
     * The default value is false.
     */
    var alwaysPassed: Boolean = false
    /**
     * Represents whether the super token is enabled or not.
     */
    var superTokenEnabled: Boolean = false
    /**
     * The superUserToken variable stores the token for the super user.
     *
     * This variable is of type String and represents the token that is used
     * for super user authentication or authorization.
     *
     * Usage example:
     *   var superUserToken: String = ""
     *
     * This variable is used in the following classes:
     *   - GoogleAttestationProperties
     *   - GoogleAttestationRequestValidationConfig
     *   - GoogleAttestationRequestValidationComponent
     *   - GoogleAttestationValidator
     *
     * See also:
     *   - GoogleAttestationProperties.superUserToken
     *   - GoogleAttestationRequestValidationConfig.getGoogleOnlineAttestationComponent()
     *   - GoogleAttestationRequestValidationConfig.getGoogleOfflineAttestationComponent()
     *   - GoogleAttestationRequestValidationComponent.superUserToken
     *   - GoogleAttestationValidator
     */
    var superUserToken: String = ""
    /**
     * Represents a key used for certain operations.
     *
     * @property key The value of the key.
     */
    var key: String = ""
    /**
     * Represents the name of the package.
     *
     * @property packageName The name of the package.
     */
    var packageName: String = ""
    /**
     * Represents the hostname configuration property.
     */
    var hostname: String = ""
    /**
     * The URI for making a request to verify Android attestations.
     */
    var uri: String = "https://www.googleapis.com/androidcheck/v1/attestations/verify"
    /**
     * Time-to-live value for a certain operation, specified in milliseconds.
     */
    var ttl: Long = 100L
    /**
     * Represents the type of the variable.
     * The available types are ONLINE and OFFLINE.
     */
    var type: TYPE = TYPE.ONLINE
    /**
     * Represents the type of validation for Google Attestation.
     */
    enum class TYPE {
        ONLINE, OFFLINE
    }
}
