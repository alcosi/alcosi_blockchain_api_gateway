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
