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

package com.alcosi.nft.apigateway.service.gateway.filter.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("opendoc")
public class OpenApiProperties {
    private Boolean disabled = false;
    private String swaggerUri = "/openapi/docs/swagger-ui/";
    private String swaggerFilePath ="classpath:com/alcosi/nft/apigateway/service/gateway/filter/openapi/swagger_html/";
    private String openApiFileUri ="/openapi/docs/openapi.yaml";
    private String openApiUri ="/openapi/docs/";

    private String openApiFilesPath ="/opt/openapi/";

    public String getOpenApiUri() {
        return openApiUri;
    }

    public void setOpenApiUri(String openApiUri) {
        this.openApiUri = openApiUri;
    }

    public String getOpenApiFilesPath() {
        return openApiFilesPath;
    }

    public void setOpenApiFilesPath(String openApiFilesPath) {
        this.openApiFilesPath = openApiFilesPath;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public void setSwaggerFilePath(String swaggerFilePath) {
        this.swaggerFilePath = swaggerFilePath;
    }

    public String getSwaggerUri() {
        return swaggerUri;
    }

    public void setSwaggerUri(String swaggerUri) {
        this.swaggerUri = swaggerUri;
    }

    public String getSwaggerFilePath() {
        return swaggerFilePath;
    }


    public String getOpenApiFileUri() {
        return openApiFileUri;
    }

    public void setOpenApiFileUri(String openApiFileUri) {
        this.openApiFileUri = openApiFileUri;
    }
}
