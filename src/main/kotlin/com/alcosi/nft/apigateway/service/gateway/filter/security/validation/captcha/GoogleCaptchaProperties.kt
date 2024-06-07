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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.captcha;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties("validation.google.captcha")
public class GoogleCaptchaProperties {
    private Boolean disabled = false;
    private Boolean alwaysPassed = false;
    private Boolean attestationSuperTokenEnabled = false;
    private String superUserToken  ="";
    private String key ="";
    private String uri="https://www.google.com/recaptcha/api/siteverify";
    private Long ttl = 6000L ;
    private TYPE type = TYPE.ONLINE;

    private BigDecimal minRate = new BigDecimal("0.3");

    public BigDecimal getMinRate() {
        return minRate;
    }

    public void setMinRate(BigDecimal minRate) {
        this.minRate = minRate;
    }

    public  enum TYPE{
        ONLINE;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getAlwaysPassed() {
        return alwaysPassed;
    }

    public void setAlwaysPassed(Boolean alwaysPassed) {
        this.alwaysPassed = alwaysPassed;
    }

    public Boolean getSuperTokenEnabled() {
        return attestationSuperTokenEnabled;
    }

    public void setSuperTokenEnabled(Boolean attestationSuperTokenEnabled) {
        this.attestationSuperTokenEnabled = attestationSuperTokenEnabled;
    }

    public String getSuperUserToken() {
        return superUserToken;
    }

    public void setSuperUserToken(String superUserToken) {
        this.superUserToken = superUserToken;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }


    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
