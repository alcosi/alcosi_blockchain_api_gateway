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
package com.alcosi.nft.apigateway.service.gateway.filter.security.validation

import org.springframework.boot.context.properties.ConfigurationProperties
/**
 * The ValidationProperties class represents the configuration properties for validation.
 * It contains various properties that can be used to customize the validation behavior.
 *
 * @property disabled Boolean flag indicating if validation is disabled. Default is false.
 * @property alwaysPassed Boolean flag indicating if all requests should always pass validation. Default is false.
 * @property tokenHeader The name of the header that contains the validation token. Default is "ValidationToken".
 * @property ipHeader The name of the header that contains the client IP address. Default is "x-real-ip".
 * @property tokenTypeHeader The name of the header that contains the token type. Default is "ValidationType".
 */
@ConfigurationProperties("validation")
open class ValidationProperties {
    /**
     * Indicates whether validation is disabled.
     */
    var disabled: Boolean = false
    /**
     * Indicates whether all requests should always pass validation.
     */
    var alwaysPassed: Boolean = false
    /**
     * The tokenHeader variable represents the name of the header that contains the validation token.
     * The default value is "ValidationToken".
     */
    var tokenHeader: String = "ValidationToken"
    /**
     * Represents the name of the header that contains the client IP address.
     */
    var ipHeader: String = "x-real-ip"
    /**
     * Represents the name of the header that contains the token type.
     */
    var tokenTypeHeader: String = "ValidationType"
}
