/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.alcosi.nft.apigateway.service.validation

import com.alcosi.lib.object_mapper.MappingHelper
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.logging.log4j.kotlin.Logging
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

open class GoogleCaptchaComponent(
    protected val captchaKey: String,
    protected val captchaEnabled: Boolean,
    protected val captchaMinRate: BigDecimal,
    protected val googleServerUrl: String,
    protected val captchaSuperTokenEnabled: Boolean,
    protected val webClient: WebClient,
    protected val mappingHelper: MappingHelper,
    protected val superUserToken: String
) : Logging {

    val noTokenResult = ValidationResult(false, BigDecimal.ZERO, "No token")
    val disabledResult = ValidationResult(true, BigDecimal.ONE, "Not active")

    data class BadResponseCaptchaException(private var s: String) : RuntimeException(s)
    open fun check(token: String?, ip: String?): Mono<ValidationResult> {
        if (!captchaEnabled) {
            return Mono.just(disabledResult)
        }
        if (token.isNullOrBlank()) {
            return Mono.just(noTokenResult)
        }
        if (superUserToken == token) {
            if (captchaSuperTokenEnabled) {
                return Mono.just(disabledResult)
            }
        }
        val body = serializeForm(Request(captchaKey, token, ip).toValueMap() as MultiValueMap<String?, String>)
        val request = webClient
            .post()
            .uri(googleServerUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body(BodyInserters.fromValue(body))
        val googleRs = request
            .exchangeToMono { res ->
                if (res.statusCode().is2xxSuccessful) {
                    res.bodyToMono<String>().map { bodyRaw ->
                        val body = mappingHelper.mapOne(bodyRaw, Response::class.java)
                            ?: throw BadResponseCaptchaException("Not valid captcha response!")
                        ValidationResult(
                            body.success && body.score() >= captchaMinRate,
                            body.score(),
                            body.errors?.joinToString(";")
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

    @JvmRecord
    data class ValidationResult(
        val success: Boolean,
        val score: BigDecimal,
        val errorDescription: String?
    )
    @JvmRecord
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    @JsonSerialize
    data class Response(
        @JsonProperty("success") val success: Boolean,
        @JsonProperty("score") private val score: BigDecimal?,
        @JsonProperty("hostname") val hostname: String?,
        @JsonProperty("action") val action: String?,
        @JsonRawValue @JsonProperty("error-codes") val errors: Set<String>?
    ) {
        fun score(): BigDecimal {
            return score ?: if (success) BigDecimal.ONE else BigDecimal.ZERO
        }
    }

    @JvmRecord
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    data class Request(
        @JsonProperty("secret") val secret: String,
        @JsonProperty("response") val response: String,
        @JsonProperty("remoteIP") val remoteIP: String?
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

    protected fun serializeForm(formData: MultiValueMap<String?, String>, charset: Charset = Charsets.UTF_8): String {
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
                })
        }
        return builder.toString()
    }

}