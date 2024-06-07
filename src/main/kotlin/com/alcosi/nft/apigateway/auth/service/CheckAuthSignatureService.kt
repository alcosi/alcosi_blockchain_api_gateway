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

import com.alcosi.lib.utils.PrepareHexService
import com.alcosi.nft.apigateway.service.exception.auth.WrongSignerException
import org.apache.commons.codec.binary.Hex
import org.apache.logging.log4j.kotlin.Logging
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.SignatureData
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * CheckAuthSignatureService is a class that provides functionality for checking the authenticity of a signature.
 *
 * @property disable A flag indicating whether the signature checking is disabled.
 * @property prepareArgsService An instance of the PrepareHexService class used for preparing hexadecimal values.
 */
open class CheckAuthSignatureService(
    protected open val disable: Boolean,
    protected open val prepareArgsService: PrepareHexService,
) : Logging {
    /**
     * Checks the authenticity of a signature.
     *
     * @param nonce The client nonce object.
     * @param signature*/
    open fun check(
        nonce: com.alcosi.nft.apigateway.auth.dto.ClientNonce,
        signature: String,
    ) {
        if (!disable) {
            val pubKey =
                Sign.signedPrefixedMessageToKey(
                    nonce.msg.toByteArray(StandardCharsets.UTF_8),
                    getSignatureData(signature),
                ).toString(16)
            val recoveredWallet = Keys.getAddress(pubKey)
            if (!recoveredWallet.equals(prepareArgsService.prepareAddr(nonce.wallet), ignoreCase = true)) {
                throw WrongSignerException(recoveredWallet, nonce.wallet)
            }
        }
    }

    /**
     * Retrieves the signature data from the given signature string.
     *
     * @param signature The signature string to extract the data from.
     * @return The extracted signature data.
     */
    protected open fun getSignatureData(signature: String): SignatureData {
        val signatureBytes = Hex.decodeHex(prepareArgsService.prepareHex(signature))
        var v = signatureBytes[64]
        if (v < 27) {
            v = (v + 27).toByte()
        }
        return SignatureData(
            v,
            Arrays.copyOfRange(signatureBytes, 0, 32),
            Arrays.copyOfRange(signatureBytes, 32, 64),
        )
    }
}
