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

package com.alcosi.nft.apigateway.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("jwt")
public class EthJwtProperties {
    private Key key = new Key();
    private Boolean checkSignDisable = false;
    private Token token = new Token();
    private Nonce nonce=new Nonce();
    private String loginTemplate = """
Please connect your wallet
@nonce@""";

    public String getLoginTemplate() {
        return loginTemplate;
    }

    public void setLoginTemplate(String loginTemplate) {
        this.loginTemplate = loginTemplate;
    }

    public Boolean getCheckSignDisable() {
        return checkSignDisable;
    }

    public void setCheckSignDisable(Boolean checkSignDisable) {
        this.checkSignDisable = checkSignDisable;
    }

    public Nonce getNonce() {
        return nonce;
    }

    public void setNonce(Nonce nonce) {
        this.nonce = nonce;
    }

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }
    public static class Nonce{
        private Duration lifetime = Duration.ofMinutes(5);
        private String redisPrefix = "LOGIN_NONCE";

        public Duration getLifetime() {
            return lifetime;
        }

        public void setLifetime(Duration lifetime) {
            this.lifetime = lifetime;
        }

        public String getRedisPrefix() {
            return redisPrefix;
        }

        public void setRedisPrefix(String redisPrefix) {
            this.redisPrefix = redisPrefix;
        }
    }

    public static class Token {
        private String issuer ="Test";
        private Duration lifetime = Duration.ofHours(1);
        private Duration rtLifetime = Duration.ofDays(7);

        public Duration getRtLifetime() {
            return rtLifetime;
        }

        public void setRtLifetime(Duration rtLifetime) {
            this.rtLifetime = rtLifetime;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public Duration getLifetime() {
            return lifetime;
        }

        public void setLifetime(Duration lifetime) {
            this.lifetime = lifetime;
        }
    }

    public static class Key {
        private String privateKey;

        public String getPrivate() {
            return getPrivateKey();
        }

        public void setPrivate(String privateKey) {
            setPrivateKey(privateKey);
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }
}
