package com.alcosi.nft.apigateway.service.gateway.filter.security.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("validation")
public class ValidationProperties {
    private Boolean disabled = false;
    private Boolean alwaysPassed = false;
    private String tokenHeader = "ValidationToken";
    private String ipHeader="x-real-ip";
    private String tokenTypeHeader="ValidationType";

    public String getTokenHeader() {
        return tokenHeader;
    }

    public void setTokenHeader(String tokenHeader) {
        this.tokenHeader = tokenHeader;
    }

    public String getIpHeader() {
        return ipHeader;
    }

    public void setIpHeader(String ipHeader) {
        this.ipHeader = ipHeader;
    }

    public String getTokenTypeHeader() {
        return tokenTypeHeader;
    }

    public void setTokenTypeHeader(String tokenTypeHeader) {
        this.tokenTypeHeader = tokenTypeHeader;
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
}
