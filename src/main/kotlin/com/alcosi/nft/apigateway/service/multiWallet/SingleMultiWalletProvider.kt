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

package com.alcosi.nft.apigateway.service.multiWallet

import com.alcosi.lib.utils.PrepareHexService
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

/**
 * Provides a single implementation of the MultiWalletProvider interface.
 *
 * @param hexService The instance of PrepareHexService used for preparing wallet addresses.
 */
open class SingleMultiWalletProvider(protected val hexService: PrepareHexService, ) : MultiWalletProvider {
    /**
     * Retrieves the list of wallet addresses associated with the given wallet name.
     *
     * @param wallet The wallet name.
     * @return A Mono emitting the MultiWalletConfig object containing the list of wallet addresses and profile ID.
     */
    override fun getWalletsListByWallet(wallet: String): Mono<MultiWalletProvider.MultiWalletConfig> {
        val preparedWallet = hexService.prepareAddr(wallet)
        return mono { MultiWalletProvider.MultiWalletConfig(listOf(preparedWallet), preparedWallet) }
    }
}
