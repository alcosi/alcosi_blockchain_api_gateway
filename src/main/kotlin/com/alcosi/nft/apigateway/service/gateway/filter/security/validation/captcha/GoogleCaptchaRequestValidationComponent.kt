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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.captcha

import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.nft.apigateway.service.error.exceptions.ApiValidationException
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.RequestValidationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationResult
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.function.Consumer

/**
 * This class is responsible for validating Google reCAPTCHA requests.
 *
 * @property alwaysPassed Indicates whether all requests should always pass validation.
 * @property superTokenEnabled Indicates whether a super user token is enabled.
 * @property superUserToken The super user token used for validation.
 * @property ttl The time-to-live (expiration) duration for unique tokens.
 * @property captchaKey The Google reCAPTCHA site key.
 * @property captchaMinRate The minimum score required for a successful validation.
 * @property googleServerUrl The URL of the Google reCAPTCHA server.
 * @property webClient The WebClient used for making HTTP requests.
 * @property mappingHelper The MappingHelper used for JSON mapping.
 * @property uniqueTokenChecker The ValidationUniqueTokenChecker used for checking token uniqueness.
 * @constructor Initializes the GoogleCaptchaRequestValidationComponent.
 */
open class GoogleCaptchaRequestValidationComponent(
    alwaysPassed: Boolean,
    superTokenEnabled: Boolean,
    superUserToken: String,
    ttl: Long,
    protected val captchaKey: String,
    protected val captchaMinRate: BigDecimal,
    protected val googleServerUrl: String,
    protected val webClient: WebClient,
    protected val mappingHelper: ObjectMapper,
    uniqueTokenChecker: ValidationUniqueTokenChecker,
) : RequestValidationComponent(alwaysPassed, superTokenEnabled, superUserToken, ttl, uniqueTokenChecker) {
    /**
     * Represents an exception that occurs when the response from the captcha service is deemed bad or invalid.
     *
     * @param s The error message associated with the exception.
     */
    data class BadResponseCaptchaException(private var s: String) : ApiValidationException(s, 1)

    /**
     * Checks the internal validation of the token and IP address.
     *
     * @param token The token string.
     * @param ip The IP address string.
     * @return A Mono emitting a ValidationResult.
     */
    override fun checkInternal(
        token: String,
        ip: String?,
    ): Mono<ValidationResult> {
        val body = serializeForm(Request(captchaKey, token, ip).toValueMap() as MultiValueMap<String?, String>)
        val request =
            webClient
                .post()
                .uri(googleServerUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromValue(body))
        val googleRs =
            request
                .exchangeToMono { res ->
                    if (res.statusCode().is2xxSuccessful) {
                        res.bodyToMono<String>().map { bodyRaw ->
                            val body =
                                mappingHelper.mapOne(bodyRaw, Response::class.java)
                                    ?: throw BadResponseCaptchaException("Not valid captcha response!")
                            ValidationResult(
                                body.success && body.score() >= captchaMinRate,
                                body.score(),
                                body.errors?.joinToString(";"),
                            )
                        }
                    } else {
                        Mono.defer { throw BadResponseCaptchaException("Not valid captcha response!") }
                    }
                }.onErrorResume { t ->
                    logger.info("Error!", t)
                    Mono.just(ValidationResult(false, BigDecimal.ZERO, "${t.javaClass.simpleName}:${t.message}"))
                }
        return googleRs
    }

    /**
     * Response represents the response received from the captcha service.
     *
     * @property success Whether the captcha verification was successful
     * @property score The score of the captcha verification (nullable)
     * @property hostname The hostname associated with the captcha response
     * @property action The action associated with the captcha response
     * @property errors The set of error codes associated with the captcha response (nullable)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    @JsonSerialize
    data class Response(
        @JsonProperty("success") val success: Boolean,
        @JsonProperty("score") private val score: BigDecimal?,
        @JsonProperty("hostname") val hostname: String?,
        @JsonProperty("action") val action: String?,
        @JsonRawValue @JsonProperty("error-codes") val errors: Set<String>?,
    ) {
        fun score(): BigDecimal {
            return score ?: if (success) BigDecimal.ONE else BigDecimal.ZERO
        }
    }

    /**
     * Request is a data class representing a request object.
     *
     * @property secret The secret string.
     * @property response The response string.
     * @property remoteIP The remote IP address (nullable).
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    data class Request(
        @JsonProperty("secret") val secret: String,
        @JsonProperty("response") val response: String,
        @JsonProperty("remoteIP") val remoteIP: String?,
    ) {
        fun toValueMap(): MultiValueMap<String, String> {
            val params: MultiValueMap<String, String> = LinkedMultiValueMap()
            params.add("secret", secret)
            params.add("response", response)
            if (remoteIP != null) {
                params.add("remoteip", remoteIP)
            }
            return params
        }
    }

    /**
     * Serializes a form data object to a URL-encoded string.
     *
     * @param formData The form data represented as a MultiValueMap.
     * @param charset The character encoding to be used in the serialization. Default is UTF-8.
     * @return The serialized form data as a URL-encoded string.
     */
    protected open fun serializeForm(
        formData: MultiValueMap<String?, String>,
        charset: Charset = Charsets.UTF_8,
    ): String {
        val builder = StringBuilder()
        formData.forEach { (name: String?, values: List<String?>) ->
            values.forEach(
                Consumer { value: String? ->
                    if (builder.isNotEmpty()) {
                        builder.append('&')
                    }
                    builder.append(URLEncoder.encode(name, charset))
                    if (value != null) {
                        builder.append('=')
                        builder.append(URLEncoder.encode(value, charset))
                    }
                },
            )
        }
        return builder.toString()
    }
}
