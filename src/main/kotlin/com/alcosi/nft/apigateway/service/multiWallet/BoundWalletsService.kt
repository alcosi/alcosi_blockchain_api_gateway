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

import com.alcosi.nft.apigateway.auth.dto.EthClient
import reactor.core.publisher.Mono

/**
 * Represents a service for binding wallets in the Ethereum client.
 */
interface BoundWalletsService {
    /**
     * Binds a wallet in the Ethereum client.
     *
     * @param client The instance of EthClient representing the Ethereum client.
     * @param walletSecond The wallet address to be bound.
     * @return A Mono emitting a String representing the result of the binding operation.
     */
    fun bound(
        client: EthClient,
        walletSecond: String,
    ): Mono<String>
}
