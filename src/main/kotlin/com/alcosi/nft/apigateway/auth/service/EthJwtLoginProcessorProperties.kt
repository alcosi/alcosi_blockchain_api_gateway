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
import org.springframework.http.HttpMethod;

import java.util.List;

@ConfigurationProperties("gateway.default-request-login-request-process")
public class EthJwtLoginProcessorProperties {
    private Boolean enabled = true;
    private String serviceUri;
    private HttpMethod serviceMethod;
    private List<LoginRequestProcess.RequestType> rqTypes = List.of();
    private List< LoginRequestProcess.TYPE> types = List.of();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceUri() {
        return serviceUri;
    }

    public void setServiceUri(String serviceUri) {
        this.serviceUri = serviceUri;
    }

    public HttpMethod getServiceMethod() {
        return serviceMethod;
    }

    public void setServiceMethod(HttpMethod serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    public List<LoginRequestProcess.RequestType> getRqTypes() {
        return rqTypes;
    }

    public void setRqTypes(List<LoginRequestProcess.RequestType> rqTypes) {
        this.rqTypes = rqTypes;
    }

    public List<LoginRequestProcess.TYPE> getTypes() {
        return types;
    }

    public void setTypes(List<LoginRequestProcess.TYPE> types) {
        this.types = types;
    }
}
