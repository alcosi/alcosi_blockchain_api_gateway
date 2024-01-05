package com.alcosi.nft.apigateway.config.path.dto

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.predicate.FilterMatchPredicate
import com.alcosi.nft.apigateway.service.predicate.PredicateMatcherType
import com.alcosi.nft.apigateway.service.predicate.SecurityConfigGatewayPredicate
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JvmRecord
data class SecurityRouteConfigDTO @JsonCreator constructor(
    val method: METHOD,
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val matches: List<FilterMatchConfigDTO>,
    val matchType: PredicateMatcherType,
    val type: PathConfigurationComponent.PREDICATE_TYPE?,
    val basePath: String,
    val baseAuthorities:List<String>,
    val addBasePath: Boolean
) {

    fun toPredicate(): SecurityConfigGatewayPredicate {
        val predicateType = type ?: PathConfigurationComponent.PREDICATE_TYPE.REGEX
        val prefix = if (addBasePath) basePath else ""
        val filterPredicate = FilterMatchPredicate(
            prefix,
            matches,
            predicateType,
            matchType,
            PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD,
            baseAuthorities
        )
        return SecurityConfigGatewayPredicate(filterPredicate, this, PathConfigurationComponent.ATTRIBUTE_SECURITY_CONFIG_FIELD)
    }
    enum class METHOD{
        ETH_JWT,IDENTITY_SERVER
    }
}