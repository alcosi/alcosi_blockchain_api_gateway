package com.alcosi.nft.apigateway.config.dto

import com.alcosi.nft.apigateway.config.PathConfig
import com.alcosi.nft.apigateway.service.predicate.FilterMatchPredicate
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType
import com.alcosi.nft.apigateway.service.predicate.RouteConfigGatewayPredicate
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JvmRecord
data class ProxyRouteConfigDTO @JsonCreator constructor(
    val name: String?,
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val matches: List<FilterMatchConfigDTO>,
    val microserviceUri: String,
    val matchType: PredicateMatcherType?,
    val type: PathConfig.PREDICATE_TYPE?,
    val basePathFilter: Boolean?,
    val order: Int?,
    val basePath: String?,
    val addBasePath: Boolean?,
    val encryptFields: List<String>?,
    val apiKey: String?
) {

    fun toPredicate(): RouteConfigGatewayPredicate {
        val predicateType = type ?: PathConfig.PREDICATE_TYPE.MVC
        val prefix = if (addBasePath == false) "" else (basePath ?: "")
        val filterPredicate = FilterMatchPredicate(
            prefix,
            matches,
            predicateType,
            matchType ?: PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST,
            PathConfig.ATTRIBUTE_REQ_AUTHORITIES_FIELD,
            listOf()
        )
        return RouteConfigGatewayPredicate(filterPredicate, this, PathConfig.ATTRIBUTE_PROXY_CONFIG_FIELD)
    }
}