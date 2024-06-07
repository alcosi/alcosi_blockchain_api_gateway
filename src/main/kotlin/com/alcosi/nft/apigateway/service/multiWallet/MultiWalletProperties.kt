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

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpMethod

/**
 * Configuration properties for the MultiWallet component.
 *
 * @property disabled Indicates whether the MultiWallet component is disabled or not. Defaults to true.
 * @property httpService Configuration properties for the HTTP service used as the MultiWallet provider.
 * @property bound Configuration properties for the bound wallet service.
 * @property provider The provider for the MultiWallet component. Currently only supports HTTP_SERVICE.
 */
@ConfigurationProperties("auth.multi-wallet")
class MultiWalletProperties {
    /**
     * Indicates whether the MultiWallet component is disabled or not.
     *
     * @property disabled The current status of the MultiWallet component.
     */
    var disabled: Boolean = true
    /**
     * Represents an instance of the HttpService class used for HTTP communication.
     */
    var httpService: HttpService = HttpService()
    /**
     * The `bound` variable represents an instance of the `HttpService` class.
     *
     * @property bound The `HttpService` instance that is bound to this variable.
     */
    var bound: HttpService = HttpService()
    /**
     * The provider for the MultiWallet component.
     */
    var provider: Provider = Provider.HTTP_SERVICE
    /**
     * Represents the provider for the MultiWallet component.
     */
    enum class Provider {
        HTTP_SERVICE
    }

    /**
     * Represents a class for making HTTP requests.
     */
    open class HttpService {
        /**
         * Represents a URI (uniform resource identifier) used for making HTTP requests.
         */
        var uri: String = ""
        /**
         * Represents the HTTP method used for making a request.
         */
        var method: HttpMethod = HttpMethod.GET
    }
}
