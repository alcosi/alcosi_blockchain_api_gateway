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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.iosDeviceCheck;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("validation.ios.device-check")
public class IOSDeviceCheckProperties {
    private Boolean disabled = false;
    private Boolean alwaysPassed = false;
    private Boolean superTokenEnabled = false;
    private String superUserToken ="";

    private String uri="https://api.devicecheck.apple.com/v1/validate_device_token";
    private Long ttl = 1000L ;
    private TYPE type = TYPE.ONLINE;
    private Jwt jwt = new Jwt();


    public static enum TYPE{
        ONLINE;
    }
    public static class Jwt {
        private String audenceUri = "https://appleid.apple.com";
        private Long ttl = 600L;
        private String keyId = "test";
        private String issuer = "test";
        private String subject = "test";
        private String privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDXFe7tTuoZ7UEKqy8XHIXfNeleS42C7FPQS8ywrWR3TA==";

        public String getAudenceUri() {
            return audenceUri;
        }

        public void setAudenceUri(String audenceUri) {
            this.audenceUri = audenceUri;
        }

        public Long getTtl() {
            return ttl;
        }

        public void setTtl(Long ttl) {
            this.ttl = ttl;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    ///
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


    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
