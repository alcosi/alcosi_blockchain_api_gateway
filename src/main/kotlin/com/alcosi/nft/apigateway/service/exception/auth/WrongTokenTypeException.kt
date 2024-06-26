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

package com.alcosi.nft.apigateway.service.exception.auth

import com.alcosi.nft.apigateway.service.error.exceptions.ApiSecurityException

/**
 * WrongTokenTypeException is a subclass of ApiSecurityException that
 * represents an exception when an unsupported token type is encountered.
 * It is thrown when a token type other than "Bearer" is received.
 *
 * @constructor Creates a WrongTokenTypeException instance.
 */
open class WrongTokenTypeException() : ApiSecurityException(
    """
Wrong token type. Only Bearer is supported
    """,
    401120,
)
