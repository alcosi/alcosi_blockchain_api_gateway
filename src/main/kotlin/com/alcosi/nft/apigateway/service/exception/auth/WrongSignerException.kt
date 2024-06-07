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
 * WrongSignerException is an open class for representing exceptions related to wrong signers.
 * It extends the ApiSecurityException class.
 *
 * @property walletSign The signer's wallet sign.
 * @property walletRq The signer's wallet request.
 *
 * @constructor Creates a WrongSignerException instance with the given wallet sign and wallet request.
 * @param walletSign The signer's wallet sign.
 * @param walletRq The signer's wallet request.
 */
open class WrongSignerException(walletSign: String, walletRq: String) : ApiSecurityException(
    """
Wrong signer. Sign: $walletSign rq:$walletRq
    """,
    401100,
)
