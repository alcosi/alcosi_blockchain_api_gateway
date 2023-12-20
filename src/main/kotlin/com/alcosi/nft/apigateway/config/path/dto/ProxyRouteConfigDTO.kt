package com.alcosi.nft.apigateway.config.path.dto

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.predicate.FilterMatchPredicate
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType
import com.alcosi.nft.apigateway.service.predicate.RouteConfigGatewayPredicate
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JvmRecord
data class ProxyRouteConfigDTO
    @JsonCreator
    constructor(
        val name: String?,
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
        val matches: List<FilterMatchConfigDTO>,
        val microserviceUri: String,
        val matchType: PredicateMatcherType?,
        val type: PathConfigurationComponent.PredicateType?,
        val basePathFilter: Boolean?,
        val order: Int?,
        val basePath: String?,
        val addBasePath: Boolean?,
        val encryptFields: List<String>?,
        val apiKey: String?,
    ) {
        fun toPredicate(): RouteConfigGatewayPredicate {
            val predicateType = type ?: PathConfigurationComponent.PredicateType.MVC
            val prefix = if (addBasePath == false) "" else (basePath ?: "")
            val filterPredicate =
                FilterMatchPredicate(
                    false,
                    prefix,
                    matches,
                    predicateType,
                    matchType ?: PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST,
                    PathAuthorities(listOf( PathAuthority(listOf(),PathAuthority.AuthoritiesCheck.ANY))),
                )
            return RouteConfigGatewayPredicate(filterPredicate, this)
        }
    }
