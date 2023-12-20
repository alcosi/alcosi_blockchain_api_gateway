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
