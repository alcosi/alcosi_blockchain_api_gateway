package com.alcosi.nft.apigateway.service.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("gateway.base")
public class GatewayBasePathProperties {
    private String path="/api";
    private String fakeUri="http://127.0.200.1:87787";

    public String getFakeUri() {
        return fakeUri;
    }

    public void setFakeUri(String fakeUri) {
        this.fakeUri = fakeUri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
