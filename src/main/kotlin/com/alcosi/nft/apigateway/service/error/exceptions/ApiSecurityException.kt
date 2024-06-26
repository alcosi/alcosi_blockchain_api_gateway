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

package com.alcosi.nft.apigateway.service.error.exceptions

/**
 * ApiSecurityException is an open class for representing API security exceptions.
 * It extends the ApiException class.
 *
 * @property code The code associated with the exception.
 * @property message The error message associated with the exception.
 *
 * @constructor Creates an ApiSecurityException instance with the given code and message.
 * @param code The code associated with the exception.
 * @param message The error message associated with the exception.
 *
 * @constructor Creates an ApiSecurityException instance with the given message and code.
 * @param message The error message associated with the exception.
 * @param code The code associated with the exception.
 */
open class ApiSecurityException(code: Long, message: String) : ApiException(code, message, 401) {
    constructor(message: String, code: Int) : this(code.toLong(), message)
}
