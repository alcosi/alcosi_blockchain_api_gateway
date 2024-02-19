/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
                    return@map it.copy(authorities = pathAuthorities.list)
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
