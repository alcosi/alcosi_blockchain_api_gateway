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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.attestation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("validation.google.attestation")
public class GoogleAttestationProperties {
    private Boolean disabled = false;
    private Boolean alwaysPassed = false;
    private Boolean superTokenEnabled = false;
    private String superUserToken ="";
    private String key ="";
    private String packageName="" ;
    private String hostname ="" ;
    private String uri="https://www.googleapis.com/androidcheck/v1/attestations/verify";
    private Long ttl = 100L ;
    private TYPE type = TYPE.ONLINE;

    public static enum TYPE{
        ONLINE,OFFLINE;
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
        return superTokenEnabled;
    }

    public void setSuperTokenEnabled(Boolean superTokenEnabled) {
        this.superTokenEnabled = superTokenEnabled;
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
