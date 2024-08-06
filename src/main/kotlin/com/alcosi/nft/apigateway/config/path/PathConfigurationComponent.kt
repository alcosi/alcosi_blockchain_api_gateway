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

package com.alcosi.nft.apigateway.config.path

import com.alcosi.lib.objectMapper.mapList
import com.alcosi.lib.objectMapper.mapOne
import com.alcosi.nft.apigateway.config.path.dto.*
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * The PathConfigurationComponent class represents a component that handles path configuration for the application.
 *
 * @property properties The path configuration properties.
 * @property securityRouteProperties The security route configuration properties.
 * @property validationRouteProperties The validation route configuration properties.
 * @property helper The mapping helper for mapping JSON objects.
 * @property objectMapper The object mapper for JSON deserialization.
 * @property basePath The base path for filtering requests.
 * @property proxyConfig The list of proxy route configurations.
 * @property securityConfig The security route configuration.
 * @property validationConfig The validation route configuration.
 */
open class PathConfigurationComponent(
    val properties: PathConfigurationProperties,
    val securityRouteProperties: SecurityRoutesProperties.SecurityRouteConfig,
    val validationRouteProperties: SecurityRoutesProperties.SecurityRouteConfig,
    val helper: ObjectMapper,
    val objectMapper: ObjectMapper,
    val basePath: String,
) {
    /**
     * Represents the lazy-loaded property that holds the configuration for a proxy route in the application.
     */
    open val proxyConfig by lazy { getProxyConfig(properties.proxy).sortedBy { it.order ?: 0 } }

    /**
     * Represents the lazy-initialized property for security configuration.
     *
     * @property securityConfig The lazy-initialized security configuration property.
     * @constructor Initializes the security configuration property using lazy initialization.
     */
    open val securityConfig by lazy { getSecurityConfig(properties.security) }

    /**
     * Lazily initializes and returns the validation configuration based on the properties provided.
     * The configuration is obtained by calling the 'getValidationConfig' method of the containing class instance.
     *
     * @return The validation configuration.
     */
    open val validationConfig by lazy { getValidationConfig(properties.validation) }

    /**
     * Retrieves the proxy route configurations based on the provided map of key-value pairs.
     *
     * @param map The map containing the proxy route configurations. The keys represent the route names and the values are JSON strings representing the route configurations.
     * @return The list of ProxyRouteConfigDTO objects representing the proxy route configurations.
     */
    protected open fun getProxyConfig(map: Map<String, String?>): List<ProxyRouteConfigDTO> {
        val result =
            map.flatMap {
                if (it.value.isNullOrBlank()) {
                    return@flatMap listOf()
                }
                val isArray = objectMapper.readTree(it.value).isArray
                val list =
                    if (isArray) {
                        helper.mapList(it.value, ProxyRouteConfigDTO::class.java)
                            .mapIndexed { ind, config -> config.copy(name = "${it.key}[$ind]", basePath = basePath) }
                    } else {
                        listOf(
                            helper.mapOne(
                                it.value,
                                ProxyRouteConfigDTO::class.java,
                            ),
                        ).map { config -> config!!.copy(name = it.key, basePath = basePath) }
                    }
                return@flatMap list
            }
        return result
    }

    /**
     * Retrieves the validation route configuration based on the provided map of key-value pairs.
     *
     * @param map The map containing the validation route configuration. The keys represent the route names and the values are JSON strings representing the route configurations.
     * @return The SecurityRouteConfigDTO object representing the validation route configuration.
     */
    protected open fun getValidationConfig(map: Map<String, Any?>): SecurityRouteConfigDTO {
        return getSecurityConfig(map, validationRouteProperties)
    }

    /**
     * Retrieves the security route configuration based on the provided map of key-value pairs.
     *
     * @param map The map containing the security route configuration. The keys represent the route names and the values are JSON strings representing the route configurations.
     * @return The SecurityRouteConfigDTO object representing the security route configuration.
     */
    protected open fun getSecurityConfig(map: Map<String, Any?>): SecurityRouteConfigDTO {
        return getSecurityConfig(map, securityRouteProperties)
    }

    /**
     * Retrieves the security route configuration based on the provided map of key-value pairs.
     *
     * @param map The map containing the security route configuration. The keys represent the route names and the values are JSON strings representing the route configurations.
     * @param properties The properties of the security route configuration.
     * @return The SecurityRouteConfigDTO object representing the security route configuration.
     */
    protected open fun getSecurityConfig(
        map: Map<String, Any?>,
        properties: SecurityRoutesProperties.SecurityRouteConfig,
    ): SecurityRouteConfigDTO {
        val addBasePath = (map["addBasePath"] as String?)?.let { it.toBoolean() } ?: false
        val typeMap = map["type"] as Map<String, String?>?
        val baseAuthorities = objectMapper.readValue(properties.baseAuthorities, PathAuthority::class.java.arrayType()) as Array<PathAuthority>?
        val pathAuthorities = PathAuthorities(baseAuthorities?.toList() ?: listOf())
        if (typeMap == null) {
            return SecurityRouteConfigDTO(
                properties.type.method,
                listOf(),
                properties.type.match,
                null,
                basePath,
                pathAuthorities,
                addBasePath,
            )
        }
        val pathList =
            if (map["path"] is Map<*, *>) {
                map["path"] as Map<String, String?>
            } else {
                mapOf("" to (map["path"] as String?))
            }.values
        val matches =
            pathList.flatMap {
                if (it.isNullOrBlank()) {
                    return@flatMap listOf()
                }
                val isArray = objectMapper.readTree(it).isArray
                val list =
                    if (isArray) {
                        helper.mapList(it, FilterMatchConfigDTO::class.java)
                    } else {
                        listOf(
                            helper.mapOne(it, FilterMatchConfigDTO::class.java)!!,
                        )
                    }
                return@flatMap list
            }.map {
                if (it.authorities().noAuth()) {
                    return@map it.copy(authoritiesGroups = pathAuthorities.pathAuthorityList)
                } else {
                    return@map it
                }
            }
        return SecurityRouteConfigDTO(
            properties.type.method,
            matches,
            properties.type.match,
            properties.type.predicate,
            basePath,
            pathAuthorities,
            addBasePath,
        )
    }

    /**
     * An enumeration representing the type of predicate used in the PathConfigurationComponent class.
     *
     * The PredicateType enum defines the possible types of predicates that can be used to match routes in the PathConfigurationComponent class.
     * The available types are:
     * - MVC: Matches routes using the Model-View-Controller design pattern.
     * - REGEX: Matches routes using regular expressions.
     */
    enum class PredicateType {
        MVC,
        REGEX,
    }

    /**
     * Enumeration representing different methods.
     */
    enum class Method {
        ETH_JWT,
        IDENTITY_SERVER,
    }

    /**
     * This companion object defines constants related to attribute fields used in various classes.
     * These fields are used as keys in attribute maps to store and retrieve values.
     */
    companion object {
        val ATTRIBUTE_PROXY_CONFIG_FIELD: String = "ATTRIBUTES_PROXY_CONFIG"
        val ATTRIBUTE_SECURITY_CONFIG_FIELD: String = "ATTRIBUTE_SECURITY_CONFIG_FIELD"
        val ATTRIBUTE_REQ_AUTHORITIES_FIELD = "ATTRIBUTE_REQ_AUTHORITIES_FIELD"
        val ATTRIBUTES_REQUEST_TIME = "ATTRIBUTES_REQUEST_TIME"
        val ATTRIBUTES_REQUEST_HISTORY_INFO = "ATTRIBUTES_REQUEST_HISTORY_INFO"
        val ATTRIBUTE_ALLOWED_HTTP_METHODS_SET = "ATTRIBUTE_ALLOWED_HTTP_METHODS_SET"
    }
}
