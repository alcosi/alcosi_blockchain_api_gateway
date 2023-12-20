package com.alcosi.nft.apigateway.service.multiWallet;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

@ConfigurationProperties("auth.multi-wallet")
public class MultiWalletProperties {
    private Boolean disabled = true;
    private HttpService httpService = new HttpService();
    private HttpService bound = new HttpService();

    private Provider provider =Provider.HTTP_SERVICE;

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public HttpService getBound() {
        return bound;
    }

    public void setBound(HttpService bound) {
        this.bound = bound;
    }

    public HttpService getHttpService() {
        return httpService;
    }

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public enum Provider{
        HTTP_SERVICE
    }

    public class HttpService{
        private String uri = "";
        private HttpMethod method = HttpMethod.GET;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public HttpMethod getMethod() {
            return method;
        }

        public void setMethod(HttpMethod method) {
            this.method = method;
        }
    }
}
