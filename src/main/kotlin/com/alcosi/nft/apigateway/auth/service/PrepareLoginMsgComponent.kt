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

import org.apache.commons.text.StringSubstitutor

/**
 * Component class responsible for preparing login messages.
 *
 * @param loginTemplate The template for login messages.
 */
open class PrepareLoginMsgComponent(val loginTemplate: String) {
    /**
     * Retrieves a formatted login message based on the provided wallet and nonce.
     *
     * @param wallet The wallet associated with the login message.
     * @param nonce The nonce associated with the login message.
     * @return The formatted login message as a [String].
     */
    open fun getMsg(
        wallet: String,
        nonce: String,
    ): String {
        val values = mapOf("nonce" to nonce, "wallet" to wallet)
        val rs = StringSubstitutor.replace(loginTemplate, values, "@", "@")
        return rs
    }
}
