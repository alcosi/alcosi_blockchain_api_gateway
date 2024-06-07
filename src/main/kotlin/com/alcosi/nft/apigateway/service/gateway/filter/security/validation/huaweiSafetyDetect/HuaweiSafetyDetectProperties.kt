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
package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.huaweiSafetyDetect

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Huawei Safety Detect.
 */
@ConfigurationProperties("validation.huawei.safety-detect")
class HuaweiSafetyDetectProperties {
    /**
     * Represents the disabled state.
     *
     * @property disabled The boolean value indicating whether the feature is disabled.
     */
    var disabled: Boolean = false
    /**
     * Indicates whether the safety detect request validation component should always pass the validation or not.
     *
     * When set to true, the safety detect request validation component will always consider the request as valid and the validation will pass.
     * When set to false, the safety detect request validation component will perform the actual validation based on other configuration properties.
     *
     * @see HuaweiSafetyRequestValidationConfig.getHuaweiSafetyDetectRequestValidationComponent
     * @see HuaweiSafetyDetectProperties
     * @see HuaweiSafetyDetectProperties.TYPE
     * @see HuaweiSafetyDetectProperties.TYPE.OFFLINE
     */
    var alwaysPassed: Boolean = false
    /**
     * Represents the configuration property for enabling the super token feature in Huawei Safety Detect.
     *
     * The `superTokenEnabled` property is a boolean value that determines whether the super token feature is enabled or not.
     *
     * @see HuaweiSafetyDetectProperties
     * @see HuaweiSafetyRequestValidationConfig
     * @see getHuaweiSafetyDetectRequestValidationComponent
     * @see TYPE
     */
    var superTokenEnabled: Boolean = false
    /**
     * SuperUserToken property is used to store the token for a superuser.
     * This token can be used for performing privileged operations that are only allowed for superusers.
     * The token is a string value.
     */
    var superUserToken: String = ""
    /**
     * Represents the package name for the Huawei Safety Detect validation.
     *
     * The package name is a string value that serves as an identifier for an Android application. It is used in the Huawei Safety Detect validation component to determine the package
     *  name of the application being validated.
     *
     * @see HuaweiSafetyDetectProperties
     * @see HuaweiSafetyRequestValidationConfig
     * @see getHuaweiSafetyDetectRequestValidationComponent
     */
    var packageName: String = ""
    /**
     * Time to live (TTL) for Huawei Safety Detect request validation.
     *
     * The TTL determines the expiration time for a validation request. After this time has passed,
     * the request will be considered expired and validation will fail.
     *
     * @see HuaweiSafetyDetectProperties
     * @see HuaweiSafetyRequestValidationConfig
     * @see getHuaweiSafetyDetectRequestValidationComponent
     */
    var ttl: Long = 1000L
    /**
     * Represents a variable of type TYPE.
     *
     * This variable is used to store the type of the safety detect validation. The possible values for `TYPE` are:
     * - `OFFLINE`: Indicates the validation is offline.
     *
     * Example usage:
     * ```
     * var type: TYPE = TYPE.OFFLINE
     * ```
     *
     * @see HuaweiSafetyDetectProperties
     */
    var type: TYPE = TYPE.OFFLINE
    /**
     * Represents a certificate used for validation.
     *
     * @property certificate The certificate string in base64 format.
     */
    var certificate: String =
        "MIIDezCCAmOgAwIBAgIhAOotS25vQF/044nsBQTOKS+7kbbPuonw/sm7KSh707NRMA0GCSqGSIb3DQEBBQUAMFAxCTAHBgNVBAYTADEJMAcGA1UECgwAMQkwBwYDVQQLDAAxDTALBgNVBAMMBHRlc3QxDzANBgkqhkiG9w0BCQEWADENMAsGA1UEAwwEdGVzdDAeFw0yNDAxMDMxNjU3MzZaFw0zNDAxMDMxNjU3MzZaMEExCTAHBgNVBAYTADEJMAcGA1UECgwAMQkwBwYDVQQLDAAxDTALBgNVBAMMBHRlc3QxDzANBgkqhkiG9w0BCQEWADCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJecDAr6coKA6rA64prQMzTBNkRiO+eScw5lcK2izpMJOQmAs+WzE84MYuH+LJ3gAhlu1N9vDnD4flxR4I1oSiiVrSlyTW/qMKa9zgPJvVvUbwyxyztbMx0uPkR4K5P2wRwN7tYBGI/3zxpsUGq/WQ+fl6NVZ4bd5dSMZtXgZrktGqvcCR54bgQ0zmfVKTzgApPbR6lERFMgfLyH1SzWQivcDwtBxMaSgwe0FEnRLJIDL8OaDLhpFkI+Q6jL5/jHPnl2j0+oQ2sz9FZnsdFllf4SFjpuYIxUGOBDingnkTz71eIifjYrTgM3vWE96SRd0q4nyfgfl9+CuSUqxb4J3fUCAwEAAaNPME0wHQYDVR0OBBYEFP4oeVKDiFGRI30GrkHVIJpTAtgZMB8GA1UdIwQYMBaAFP4oeVKDiFGRI30GrkHVIJpTAtgZMAsGA1UdEQQEMAKCADANBgkqhkiG9w0BAQUFAAOCAQEAUdUpXnToNTpAVImCjzQzJJP9GiNwOz/UDCm8MAqxaioMYZCS5E8MJuqiUdhvTmcNxtPFGsNWdO9Dt6F9sJpUxJGQ8dKRJaf9IAUD5A0Cprdm/wKZHmXAXDnewo01Cm69gKkVKZMBexN21K/UhmRw8CBK8+8ypvxXQpQ3WowOJwMAyrQy+Hsmv1l19TAaFdyfIJjtXH3xn/FHgL1DfOWYeamGypaEp4a2ZCVNVLr5kuTn0zJrA/I2Y56kanc6xCtSTKwCktEI/tyuP4p8yLBWZtRJSlBvwglxirjhNcJhGDOUrjCOxlAIiC6BjLEXq7Qcqgsr4fUur9BXCXsI/FAIWw=="
    /**
     * Represents the type of safety detect validation.
     *
     * The possible values for this enum are:
     *  - OFFLINE: Indicates that the safety detect validation is offline.
     */
    enum class TYPE {
        OFFLINE
    }
}
