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

package com.alcosi.nft.apigateway.auth.dto

import com.alcosi.lib.security.OneAuthorityPrincipal

/**
 * Represents an Ethereum client with the current wallet, profile wallets, and profile ID information.
 * Extends the OneAuthorityPrincipal class.
 *
 * @param currentWallet The current wallet associated with the client.
 * @param profileWallets The list of profile wallets associated with the client.
 * @param profileId The profile ID associated with the client.
 */
open class EthClient(
    val currentWallet: String,
    val profileWallets: List<String>,
    val profileId: String,
) : OneAuthorityPrincipal(profileId, "ALL",EthClient::class.java.name,"ETH") {
    override fun getName(): String {
        return profileId
    }
}
