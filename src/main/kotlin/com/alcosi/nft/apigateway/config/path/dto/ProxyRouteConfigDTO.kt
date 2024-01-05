package com.alcosi.nft.apigateway.config.path.dto

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
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
    val type: PathConfigurationComponent.PREDICATE_TYPE?,
    val basePathFilter: Boolean?,
    val order: Int?,
    val basePath: String?,
    val addBasePath: Boolean?,
    val encryptFields: List<String>?,
    val apiKey: String?
) {

    fun toPredicate(): RouteConfigGatewayPredicate {
        val predicateType = type ?: PathConfigurationComponent.PREDICATE_TYPE.MVC
        val prefix = if (addBasePath == false) "" else (basePath ?: "")
        val filterPredicate = FilterMatchPredicate(
            prefix,
            matches,
            predicateType,
            matchType ?: PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST,
            PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD,
            listOf()
        )
        return RouteConfigGatewayPredicate(filterPredicate, this, PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD)
    }
}