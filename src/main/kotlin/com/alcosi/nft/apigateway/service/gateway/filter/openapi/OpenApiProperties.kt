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
package com.alcosi.nft.apigateway.service.gateway.filter.openapi

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for OpenAPI documentation.
 *
 * @property disabled Whether OpenAPI documentation is disabled.
 * @property swaggerUri The URI path for the Swagger UI.
 * @property swaggerFilePath The file path for the Swagger UI files.
 * @property openApiFileUri The URI path for the OpenAPI file.
 * @property openApiUri The URI path for accessing OpenAPI documentation.
 * @property openApiFilesPath The file path for storing OpenAPI files.
 */
@ConfigurationProperties("opendoc")
class OpenApiProperties {
    /**
     * Represents whether OpenAPI documentation is disabled.
     *
     * @property disabled Flag indicating whether OpenAPI documentation is disabled. The default value is `false`.
     */
    var disabled: Boolean = false

    /**
     * The URI path for the Swagger UI.
     */
    var swaggerUri: String = "/openapi/docs/swagger-ui/"

    /**
     * File path for the Swagger UI files.
     */
    var swaggerFilePath: String = "classpath:com/alcosi/nft/apigateway/service/gateway/filter/openapi/swagger_html/"

    /**
     * The URI path for the OpenAPI file.
     */
    var openApiFileUri: String = "/openapi/docs/openapi.yaml"

    /**
     * Represents the URI path for accessing OpenAPI documentation.
     *
     * @property openApiUri The URI path for accessing OpenAPI documentation.
     */
    var openApiUri: String = "/openapi/docs/"

    /**
     * The file path for storing OpenAPI files.
     */
    var openApiFilesPath: String = "/opt/openapi/"
}
