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

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.config.path.dto.*
import com.fasterxml.jackson.databind.ObjectMapper

open class PathConfigurationComponent(
    val properties: PathConfigurationProperties,
    val securityRouteProperties: SecurityRoutesProperties.SecurityRouteConfig,
    val validationRouteProperties: SecurityRoutesProperties.SecurityRouteConfig,
    val helper: MappingHelper,
    val objectMapper: ObjectMapper,
    val basePath: String,
) {
    open val proxyConfig by lazy { getProxyConfig(properties.proxy).sortedBy { it.order ?: 0 } }
    open val securityConfig by lazy { getSecurityConfig(properties.security) }
    open val validationConfig by lazy { getValidationConfig(properties.validation) }

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

    protected open fun getValidationConfig(map: Map<String, Any?>): SecurityRouteConfigDTO {
        return getSecurityConfig(map, validationRouteProperties)
    }

    protected open fun getSecurityConfig(map: Map<String, Any?>): SecurityRouteConfigDTO {
        return getSecurityConfig(map, securityRouteProperties)
    }

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

    enum class PredicateType {
        MVC,
        REGEX,
    }

    enum class Method {
        ETH_JWT,
        IDENTITY_SERVER,
    }

    companion object {
        val ATTRIBUTE_PROXY_CONFIG_FIELD: String = "ATTRIBUTES_PROXY_CONFIG"
        val ATTRIBUTE_SECURITY_CONFIG_FIELD: String = "ATTRIBUTE_SECURITY_CONFIG_FIELD"
        val ATTRIBUTE_REQ_AUTHORITIES_FIELD = "ATTRIBUTE_REQ_AUTHORITIES_FIELD"
        val ATTRIBUTES_REQUEST_TIME = "ATTRIBUTES_REQUEST_TIME"
        val ATTRIBUTES_REQUEST_HISTORY_INFO = "ATTRIBUTES_REQUEST_HISTORY_INFO"
    }
}
