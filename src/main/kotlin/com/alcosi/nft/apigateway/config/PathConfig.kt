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

package com.alcosi.nft.apigateway.config

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.service.predicate.FilterMatchPredicate
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType
import com.alcosi.nft.apigateway.service.predicate.RouteConfigGatewayPredicate
import com.alcosi.nft.apigateway.service.predicate.SecurityConfigGatewayPredicate
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

open class PathConfig(
    val properties: PathConfigProperties,
    val helper: MappingHelper,
    val objectMapper: ObjectMapper,
     val basePath: String,
) {
    open val proxyConfig = getProxyConfig(properties.proxy).sortedBy { it.order ?: 0 }
    open val securityConfig = getSecurityConfig(properties.security)
    open val captchaConfig = getSecurityConfig(properties.captcha)

    protected open fun getProxyConfig(map: Map<String, String?>): List<ProxyRouteConfig> {
        val result = properties.proxy.flatMap {
            if (it.value.isNullOrBlank()) {
                return@flatMap listOf()
            }
            val isArray = objectMapper.readTree(it.value).isArray
            val list = if (isArray) {
                helper.mapList(it.value, ProxyRouteConfig::class.java)
                    .mapIndexed { ind, config -> config.copy(name = "${it.key}[${ind}]", basePath = basePath) }
            } else {
                listOf(
                    helper.mapOne(
                        it.value,
                        ProxyRouteConfig::class.java
                    )
                ).map { config -> config!!.copy(name = it.key, basePath = basePath) }
            }
            return@flatMap list
        }
        return result
    }

    protected open fun getSecurityConfig(map: Map<String, Any?>): SecurityRouteConfig {
        val addBasePath = (map["addBasePath"] as String?)?.let { it.toBoolean() } ?: false
        val baseAuthorities=(map["base_authorities"] as String?)?.let { objectMapper.readTree(it)}?.let {
            if (it.isArray){
                (it as ArrayNode ).map {itt->itt.asText()  }
            } else{
                listOf(it.asText())
            }
        }?: listOf("ALL")
        val any = map["type"]
        val typeMap = any as Map<String, String?>?
        if (typeMap == null) {
            return SecurityRouteConfig(
                SecurityRouteConfig.METHOD.ETH_JWT,
                listOf(),
                PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST,
                null,
                basePath,
                baseAuthorities,
                addBasePath
            )
        }
        val predicateType = (typeMap["predicate"])?.let { PREDICATE_TYPE.valueOf(it.uppercase()) }
        val matchType = (typeMap["match"])?.let { PredicateMatcherType.valueOf(it.uppercase()) }
        val methodType = (typeMap["method"])?.let { SecurityRouteConfig.METHOD.valueOf(it.uppercase()) }?:SecurityRouteConfig.METHOD.ETH_JWT
        //,val addBasePath:Boolean?
        val pathList = if (map["path"] is Map<*, *>) {
            map["path"] as Map<String, String?>
        } else {
            mapOf("" to (map["path"] as String?))
        }.values
        val matches = pathList.flatMap {
            if (it.isNullOrBlank()) {
                return@flatMap listOf()
            }
            val isArray = objectMapper.readTree(it).isArray
            val list = if (isArray) {
                helper.mapList(it, FilterMatchConfig::class.java)
            } else {
                listOf(
                    helper.mapOne(it, FilterMatchConfig::class.java)!!
                )
            }
            return@flatMap list
        }
        return SecurityRouteConfig(methodType,matches, matchType, predicateType, basePath,baseAuthorities, addBasePath)
    }

    @JvmRecord
    data class SecurityRouteConfig @JsonCreator constructor(
        val method:METHOD,
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
        val matches: List<FilterMatchConfig>,
        val matchType: PredicateMatcherType?,
        val type: PREDICATE_TYPE?,
        val basePath: String,
        val baseAuthorities:List<String>,
        val addBasePath: Boolean
    ) {

        fun toPredicate(): SecurityConfigGatewayPredicate {
            val predicateType = type ?: PREDICATE_TYPE.REGEX
            val prefix = if (addBasePath) basePath else ""
            val filterPredicate = FilterMatchPredicate(prefix, matches, predicateType, matchType?:PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST,ATTRIBUTE_REQ_AUTHORITIES_FIELD,baseAuthorities)
            return SecurityConfigGatewayPredicate(filterPredicate, this, ATTRIBUTE_SECURITY_CONFIG_FIELD)
        }
        enum class METHOD{
            ETH_JWT,IDENTITY_SERVER
        }
    }

    @JvmRecord
    data class ProxyRouteConfig @JsonCreator constructor(
        val name: String?,
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
        val matches: List<FilterMatchConfig>,
        val microserviceUri: String,
        val matchType: PredicateMatcherType?,
        val type: PREDICATE_TYPE?,
        val basePathFilter: Boolean?,
        val order: Int?,
        val basePath: String?,
        val addBasePath: Boolean?,
        val encryptFields: List<String>?,
        val apiKey: String?
    ) {

        fun toPredicate(): RouteConfigGatewayPredicate {
            val predicateType = type ?: PREDICATE_TYPE.MVC
            val prefix = if (addBasePath == false) "" else (basePath ?: "")
            val filterPredicate = FilterMatchPredicate(prefix, matches, predicateType,matchType?:PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST, ATTRIBUTE_REQ_AUTHORITIES_FIELD, listOf())
            return RouteConfigGatewayPredicate(filterPredicate, this, ATTRIBUTE_PROXY_CONFIG_FIELD)
        }
    }

    enum class PREDICATE_TYPE {
        MVC, REGEX;
    }

    companion object {
        val ATTRIBUTE_PROXY_CONFIG_FIELD: String = "ATTRIBUTES_PROXY_CONFIG"
        val ATTRIBUTE_SECURITY_CONFIG_FIELD: String = "ATTRIBUTE_SECURITY_CONFIG_FIELD"
        val ATTRIBUTE_REQ_AUTHORITIES_FIELD = "ATTRIBUTE_REQ_AUTHORITIES_FIELD"
    }
}