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

package com.alcosi.nft.apigateway.service.gateway.filter.security.validation.huaweiSafetyDetect;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("validation.huawei.safety-detect")
public class HuaweiSafetyDetectProperties {
    private Boolean disabled = false;
    private Boolean alwaysPassed = false;
    private Boolean superTokenEnabled = false;
    private String superUserToken ="";
    private String packageName="" ;
    private Long ttl = 1000L ;
    private TYPE type = TYPE.OFFLINE;
    private String certificate="MIIDezCCAmOgAwIBAgIhAOotS25vQF/044nsBQTOKS+7kbbPuonw/sm7KSh707NRMA0GCSqGSIb3DQEBBQUAMFAxCTAHBgNVBAYTADEJMAcGA1UECgwAMQkwBwYDVQQLDAAxDTALBgNVBAMMBHRlc3QxDzANBgkqhkiG9w0BCQEWADENMAsGA1UEAwwEdGVzdDAeFw0yNDAxMDMxNjU3MzZaFw0zNDAxMDMxNjU3MzZaMEExCTAHBgNVBAYTADEJMAcGA1UECgwAMQkwBwYDVQQLDAAxDTALBgNVBAMMBHRlc3QxDzANBgkqhkiG9w0BCQEWADCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJecDAr6coKA6rA64prQMzTBNkRiO+eScw5lcK2izpMJOQmAs+WzE84MYuH+LJ3gAhlu1N9vDnD4flxR4I1oSiiVrSlyTW/qMKa9zgPJvVvUbwyxyztbMx0uPkR4K5P2wRwN7tYBGI/3zxpsUGq/WQ+fl6NVZ4bd5dSMZtXgZrktGqvcCR54bgQ0zmfVKTzgApPbR6lERFMgfLyH1SzWQivcDwtBxMaSgwe0FEnRLJIDL8OaDLhpFkI+Q6jL5/jHPnl2j0+oQ2sz9FZnsdFllf4SFjpuYIxUGOBDingnkTz71eIifjYrTgM3vWE96SRd0q4nyfgfl9+CuSUqxb4J3fUCAwEAAaNPME0wHQYDVR0OBBYEFP4oeVKDiFGRI30GrkHVIJpTAtgZMB8GA1UdIwQYMBaAFP4oeVKDiFGRI30GrkHVIJpTAtgZMAsGA1UdEQQEMAKCADANBgkqhkiG9w0BAQUFAAOCAQEAUdUpXnToNTpAVImCjzQzJJP9GiNwOz/UDCm8MAqxaioMYZCS5E8MJuqiUdhvTmcNxtPFGsNWdO9Dt6F9sJpUxJGQ8dKRJaf9IAUD5A0Cprdm/wKZHmXAXDnewo01Cm69gKkVKZMBexN21K/UhmRw8CBK8+8ypvxXQpQ3WowOJwMAyrQy+Hsmv1l19TAaFdyfIJjtXH3xn/FHgL1DfOWYeamGypaEp4a2ZCVNVLr5kuTn0zJrA/I2Y56kanc6xCtSTKwCktEI/tyuP4p8yLBWZtRJSlBvwglxirjhNcJhGDOUrjCOxlAIiC6BjLEXq7Qcqgsr4fUur9BXCXsI/FAIWw==";

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public static enum TYPE{
        OFFLINE;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
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



    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }



    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
