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
import com.alcosi.nft.apigateway.service.predicate.ApiRegexRequestMatcherPredicate
import com.alcosi.nft.apigateway.service.predicate.ApiRequestMvcMatcherPredicate
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

@Component
class PathConfig(
    val properties: PathConfigProperties,
    val helper: MappingHelper,
    val objectMapper: ObjectMapper,
    @Value("\${gateway.base.path:/api}")val basePath: String,
    ) {
    val proxyConfig = getProxyConfig(properties.proxy).sortedBy { it.order?:0 }
    val securityConfig = getSecurityConfig(properties.security)
    val captchaConfig = getSecurityConfig(properties.captcha)

    protected fun getProxyConfig(map: Map<String, String?>): List<ProxyRouteConfig> {
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

    protected fun getSecurityConfig(map: Map<String, Any?>): SecurityRouteConfig {
        val addBasePath = (map["addBasePath"] as String?)?.let { it.toBoolean()}?:false

        val any = map["type"]
        val typeMap = any as Map<String, String?>?
        if (typeMap==null){
            return SecurityRouteConfig(listOf(),null,null,basePath,addBasePath)
        }
        val predicateType = (typeMap["predicate"])?.let {   PREDICATE_TYPE.valueOf(it.uppercase()) }
        val matchType = (typeMap["match"])?.let { PredicateMatcherType.valueOf(it.uppercase()) }

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
        return SecurityRouteConfig(matches, matchType, predicateType,basePath,addBasePath)
    }

    @JvmRecord
    data class SecurityRouteConfig(
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
        val matches: List<FilterMatchConfig>,
        val matchType: PredicateMatcherType?,
        val type: PREDICATE_TYPE?,
        val basePath: String,
        val addBasePath:Boolean
    ) {
        fun toPredicate(): Predicate<ServerWebExchange> {
            val predicateMatcherType = matchType ?: PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST
            val predicateType = type ?: PREDICATE_TYPE.REGEX
            val prefix= if (addBasePath)  basePath else ""
            return when (predicateType) {
                PREDICATE_TYPE.MVC -> ApiRequestMvcMatcherPredicate(prefix,predicateMatcherType, matches)
                PREDICATE_TYPE.REGEX -> ApiRegexRequestMatcherPredicate(prefix,predicateMatcherType, matches)
            }
        }
    }

    @JvmRecord
    data class ProxyRouteConfig(
        val name: String?,
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
        val matches: List<FilterMatchConfig>,
        val microserviceUri: String,
        val matchType: PredicateMatcherType?,
        val type: PREDICATE_TYPE?,
        val basePathFilter: Boolean?,
        val order: Int?,
        val basePath: String?,
        val addBasePath:Boolean?
    ) {
        fun toPredicate(): Predicate<ServerWebExchange> {
            val predicateMatcherType = matchType ?: PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST
            val predicateType = type ?: PREDICATE_TYPE.MVC
            val prefix= if (addBasePath==false)  "" else (basePath?:"")
            return when (predicateType) {
                PREDICATE_TYPE.MVC -> ApiRequestMvcMatcherPredicate(prefix,predicateMatcherType, matches)
                PREDICATE_TYPE.REGEX -> ApiRegexRequestMatcherPredicate(prefix,predicateMatcherType, matches)
            }
        }
    }

    enum class PREDICATE_TYPE {
        MVC, REGEX;
    }
}