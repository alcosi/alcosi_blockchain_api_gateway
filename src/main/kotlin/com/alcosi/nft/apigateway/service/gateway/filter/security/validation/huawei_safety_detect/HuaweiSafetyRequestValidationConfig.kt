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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.huawei_safety_detect

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.ValidationUniqueTokenChecker
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.captcha.GoogleCaptchaRequestValidationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.validation.captcha.GoogleCaptchaValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnProperty(
    prefix = "validation.huawei.safety_detect",
    name = ["disabled"],
    matchIfMissing = true,
    havingValue = "false"
)
class HuaweiSafetyRequestValidationConfig {
    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyVerifySignatureComponent::class)
    fun getHuaweiSafetyVerifySignatureComponent(
        @Value("\${validation.huawei.safety_detect.cert:MIIDezCCAmOgAwIBAgIhAOotS25vQF/044nsBQTOKS+7kbbPuonw/sm7KSh707NRMA0GCSqGSIb3DQEBBQUAMFAxCTAHBgNVBAYTADEJMAcGA1UECgwAMQkwBwYDVQQLDAAxDTALBgNVBAMMBHRlc3QxDzANBgkqhkiG9w0BCQEWADENMAsGA1UEAwwEdGVzdDAeFw0yNDAxMDMxNjU3MzZaFw0zNDAxMDMxNjU3MzZaMEExCTAHBgNVBAYTADEJMAcGA1UECgwAMQkwBwYDVQQLDAAxDTALBgNVBAMMBHRlc3QxDzANBgkqhkiG9w0BCQEWADCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJecDAr6coKA6rA64prQMzTBNkRiO+eScw5lcK2izpMJOQmAs+WzE84MYuH+LJ3gAhlu1N9vDnD4flxR4I1oSiiVrSlyTW/qMKa9zgPJvVvUbwyxyztbMx0uPkR4K5P2wRwN7tYBGI/3zxpsUGq/WQ+fl6NVZ4bd5dSMZtXgZrktGqvcCR54bgQ0zmfVKTzgApPbR6lERFMgfLyH1SzWQivcDwtBxMaSgwe0FEnRLJIDL8OaDLhpFkI+Q6jL5/jHPnl2j0+oQ2sz9FZnsdFllf4SFjpuYIxUGOBDingnkTz71eIifjYrTgM3vWE96SRd0q4nyfgfl9+CuSUqxb4J3fUCAwEAAaNPME0wHQYDVR0OBBYEFP4oeVKDiFGRI30GrkHVIJpTAtgZMB8GA1UdIwQYMBaAFP4oeVKDiFGRI30GrkHVIJpTAtgZMAsGA1UdEQQEMAKCADANBgkqhkiG9w0BAQUFAAOCAQEAUdUpXnToNTpAVImCjzQzJJP9GiNwOz/UDCm8MAqxaioMYZCS5E8MJuqiUdhvTmcNxtPFGsNWdO9Dt6F9sJpUxJGQ8dKRJaf9IAUD5A0Cprdm/wKZHmXAXDnewo01Cm69gKkVKZMBexN21K/UhmRw8CBK8+8ypvxXQpQ3WowOJwMAyrQy+Hsmv1l19TAaFdyfIJjtXH3xn/FHgL1DfOWYeamGypaEp4a2ZCVNVLr5kuTn0zJrA/I2Y56kanc6xCtSTKwCktEI/tyuP4p8yLBWZtRJSlBvwglxirjhNcJhGDOUrjCOxlAIiC6BjLEXq7Qcqgsr4fUur9BXCXsI/FAIWw==}") cert: String ,
    ): HuaweiSafetyVerifySignatureComponent {
        return HuaweiSafetyVerifySignatureComponent(cert)
    }
    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyDetectRequestValidationComponent::class)
    fun getHuaweiSafetyDetectRequestValidationComponent(
        @Value("\${validation.huawei.safety_detect.always_passed:false}") alwaysPassed: Boolean,
        @Value("\${validation.huawei.safety_detect.super_token.enabled:false}") captchaSuperTokenEnabled: Boolean,
        @Value("\${validation.huawei.safety_detect.super_token.value:test}") superUserToken: String,
        @Value("\${validation.huawei.safety_detect.ttl:1000}")  ttl: Long,
        @Value("\${validation.huawei.safety_detect.package_name:test}") packageName: String,
        webClient: WebClient,
        mappingHelper: MappingHelper,
        verifyUtil: HuaweiSafetyVerifySignatureComponent,
        uniqueTokenChecker: ValidationUniqueTokenChecker,): HuaweiSafetyDetectRequestValidationComponent {
        return HuaweiSafetyDetectRequestValidationComponent(alwaysPassed, captchaSuperTokenEnabled, superUserToken,ttl,packageName, webClient, mappingHelper,verifyUtil,uniqueTokenChecker)
    }

    @Bean
    @ConditionalOnMissingBean(HuaweiSafetyDetectCheckValidator::class)
    fun getHuaweiSafetyDetectCheckValidator(
        component: HuaweiSafetyDetectRequestValidationComponent
    ): HuaweiSafetyDetectCheckValidator {
        return HuaweiSafetyDetectCheckValidator(component)
    }
}