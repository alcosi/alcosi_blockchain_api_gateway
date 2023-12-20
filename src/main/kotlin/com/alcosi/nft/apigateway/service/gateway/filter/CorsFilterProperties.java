package com.alcosi.nft.apigateway.service.gateway.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("gateway.filter.cors")
public class CorsFilterProperties {
    private Boolean enabled = true;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
