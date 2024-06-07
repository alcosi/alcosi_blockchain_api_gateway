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

package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("filter.config.path.security.identity-server")
public class IdentityServerProperties {
    private String uri;
    private String idApiVersion = "2.0";
    private String claimClientId = "clientId";
    private String claimOrganisationId = "organisationId";

    private String claimType= "type";
    private String claimAuthorities= "authorities";
    private String clientId;
    private String clientSecret;
    private String clientScopes;
    private String grantType;
    private String password;
    private String username;

    public String getClaimOrganisationId() {
        return claimOrganisationId;
    }

    public void setClaimOrganisationId(String claimOrganisationId) {
        this.claimOrganisationId = claimOrganisationId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientScopes() {
        return clientScopes;
    }

    public void setClientScopes(String clientScopes) {
        this.clientScopes = clientScopes;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClaimClientId() {
        return claimClientId;
    }

    public void setClaimClientId(String claimClientId) {
        this.claimClientId = claimClientId;
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public String getClaimAuthorities() {
        return claimAuthorities;
    }

    public void setClaimAuthorities(String claimAuthorities) {
        this.claimAuthorities = claimAuthorities;
    }


    public String getIdApiVersion() {
        return idApiVersion;
    }

    public void setIdApiVersion(String idApiVersion) {
        this.idApiVersion = idApiVersion;
    }
}



