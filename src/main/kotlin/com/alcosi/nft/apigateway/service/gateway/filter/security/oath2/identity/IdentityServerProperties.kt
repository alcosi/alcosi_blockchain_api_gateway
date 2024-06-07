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
package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.identity

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration class for Identity Server properties.
 *
 * This class is annotated with @ConfigurationProperties to specify the prefix for the properties and
 * to bind the properties to the fields of this class.
 *
 * @property uri The URL of the identity server.
 * @property idApiVersion The API version of the identity server.
 * @property claimClientId The claim name for the client ID.
 * @property claimOrganisationId The claim name for the organisation ID.
 * @property claimType The claim name for the claim type.
 * @property claimAuthorities The claim name for the claim authorities.
 * @property clientId The client ID.
 * @property clientSecret The client secret.
 * @property clientScopes The client scopes.
 * @property grantType The OAuth2 grant type.
 * @property password The password.
 * @property username The username.
 *
 * @constructor Creates a new instance of IdentityServerProperties.
 */
@ConfigurationProperties("filter.config.path.security.identity-server")
class IdentityServerProperties {
    /**
     * Variable representing the Uniform Resource Identifier (URI) of the Identity Server.
     *
     * This variable is nullable and its value should be a valid URI string.
     * It is used as a property in the IdentityServerProperties class and is bound to the "uri" field of the class.
     * The IdentityServerProperties class is a configuration class for Identity Server properties.
     *
     * @see IdentityServerProperties
     */
    var uri: String? = null
    /**
     * Represents the version of the API used in the application.
     */
    var idApiVersion: String = "2.0"
    /**
     * The claimClientId variable represents the client ID for a claim.
     *
     * @property claimClientId The client ID for the claim.
     */
    var claimClientId: String = "clientId"
    /**
     * Represents the ID of an organization in the claim.
     */
    var claimOrganisationId: String = "organisationId"
    /**
     * The claimType represents the type of claim in the user's profile.
     */
    var claimType: String = "type"
    /**
     * Represents the authorities claim in an OAuth2 token.
     *
     * @property claimAuthorities The value of the authorities claim.
     */
    var claimAuthorities: String = "authorities"
    /**
     * Nullable variable that holds the client ID.
     */
    var clientId: String? = null
    /**
     * The `clientSecret` variable is a nullable string that represents the client secret used for authentication with the identity server.
     * This variable is used in various classes and functions, such as `Oath2AuthComponent`, `getOath2AuthComponent`, and `IdentityOath2APIGetUserInfoComponent`.
     * It is often passed as a parameter or assigned to a property of an object.
     *
     * @see Oath2AuthComponent
     * @see getOath2AuthComponent
     * @see IdentityOath2APIGetUserInfoComponent
     */
    var clientSecret: String? = null
    /**
     * Nullable variable that represents the client scopes for the Identity Server.
     * The client scopes define the permissions and access levels granted to the client application.
     *
     * @see IdentityServerProperties
     */
    var clientScopes: String? = null
    /**
     * grantType is a nullable variable that represents the type of grant used for authentication or authorization.
     *
     * This variable is typically used in the context of OAuth2 authentication and authorization flows.
     *
     * It can have the following values:
     *  - null: If no grant type is specified.
     *  - "authorization_code": If the authorization code grant type is used.
     *  - "implicit": If the implicit grant type is used.
     *  - "password": If the resource owner password credentials grant type is used.
     *  - "client_credentials": If the client credentials grant type is used.
     *  - "refresh_token": If the refresh token grant type is used.
     *
     * This variable can be set to any of the above values as per the authentication*/
    var grantType: String? = null
    /**
     * The password variable stores a user's password.
     *
     * It is a nullable [String] type, allowing it to be assigned a null value if no password is provided.
     *
     * Example Usage:
     * ```
     * var password: String? = null
     * ```
     */
    var password: String? = null
    /**
     * The username variable stores the username of a user.
     * It is a nullable String, allowing it to be assigned a value or null.
     *
     * @see IdentityServerProperties
     * @see Oath2FiltersConfig
     * @see getOath2AuthComponent
     * @see SecurityFiltersConfig
     * @see IdentityOath2APIGetUserInfoComponent
     *
     * @sample IdentityOath2APIGetUserInfoComponent.getInfo
     */
    var username: String? = null
}



