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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.attestation

import com.alcosi.nft.apigateway.service.error.exceptions.ApiValidationException
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.RequestValidationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * This class represents a component for validating Google Attestation requests.
 *
 * @param alwaysPassed A boolean indicating if the validation always passes.
 * @param superTokenEnabled A boolean indicating if the super token is enabled.
 * @param superUserToken The super user token string.
 * @param key The key string.
 * @param packageName The package name string.
 * @param ttl The time-to-live value in milliseconds.
 * @param mappingHelper The MappingHelper instance.
 * @param uniqueTokenChecker The ValidationUniqueTokenChecker instance.
 */
abstract class GoogleAttestationRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    val key: String,
    val packageName: String,
    val ttl: Long,
    val mappingHelper: ObjectMapper,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : RequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, ttl, uniqueTokenChecker) {
    data class AttestationValidationException(var s: String) : ApiValidationException(s, 2)
}
