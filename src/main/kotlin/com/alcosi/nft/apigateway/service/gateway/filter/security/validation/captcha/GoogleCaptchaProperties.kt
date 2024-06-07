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
package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.captcha

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

/**
 * Configuration properties for Google Captcha validation.
 *
 * @property disabled Whether Google Captcha validation is disabled. Default: false.
 * @property alwaysPassed Whether Google Captcha validation always passes. Default: false.
 * @property superTokenEnabled Whether to enable super user token for Google Captcha validation. Default: false.
 * @property superUserToken The super user token for Google Captcha validation.
 * @property key The API key for Google Captcha validation.
 * @property uri The URI for the Google Captcha verification endpoint. Default: "https*/
@ConfigurationProperties("validation.google.captcha")
open class GoogleCaptchaProperties {
    /**
     * Represents whether Google Captcha validation is disabled.
     */
    var disabled: Boolean = false
    /**
     * Represents a variable indicating whether a condition is always passed.
     *
     * @property alwaysPassed Boolean value indicating whether the condition is always passed.
     */
    var alwaysPassed: Boolean = false
    /**
     * Whether the super token functionality is enabled or not.
     */
    var superTokenEnabled: Boolean = false
    /**
     * The superUserToken is a variable that stores a String representation of a super user token.
     * It is used to authenticate and identify super users in the system.
     */
    var superUserToken: String = ""
    /**
     * Represents the API key used for Google Captcha validation.
     * Default value is an empty string.
     */
    var key: String = ""
    /**
     * Represents the URI (Uniform Resource Identifier) for the Google Captcha API endpoint.
     */
    var uri: String = "https://www.google.com/recaptcha/api/siteverify"
    /**
     * Time to Live (TTL) for a specific operation.
     *
     * @property ttl The duration in milliseconds that represents the time to live.
     */
    var ttl: Long = 6000L
    /**
     * Represents the type of a variable.
     *
     * Currently, there is only one possible value for this variable, which is "ONLINE".
     * This indicates that the variable is used to represent an online state.
     *
     * @see GoogleCaptchaProperties
     */
    var type: TYPE = TYPE.ONLINE
    /**
     * Represents the minimum interest rate.
     *
     * The `minRate` variable stores the minimum interest rate as a `BigDecimal` value.
     * The default value is 0.3.
     *
     * @see BigDecimal
     */
    var minRate: BigDecimal = BigDecimal("0.3")
    /**
     * Enum representing the type of validation for Google Captcha.
     *
     * There is only one value in this enum:
     * - ONLINE: Indicates that the validation is done online.
     */
    enum class TYPE {
        ONLINE
    }
}
