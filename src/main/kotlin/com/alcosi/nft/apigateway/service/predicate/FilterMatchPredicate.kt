package com.alcosi.nft.apigateway.service.predicate

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO
import com.alcosi.nft.apigateway.config.path.dto.PathAuthorities
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcher
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherMVC
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherRegex
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

open class FilterMatchPredicate(
    val isSecured: Boolean,
    prefix: String,
    pathMethods: List<FilterMatchConfigDTO>,
    val type: PathConfigurationComponent.PredicateType,
    val matchType: PredicateMatcherType,
    val baseAuthorities: PathAuthorities,
    ) : Logging, Predicate<ServerWebExchange> {
    open val matchers: List<HttpFilterMatcher<*>> =
        when (type) {
            PathConfigurationComponent.PredicateType.REGEX -> pathMethods.map { HttpFilterMatcherRegex(prefix, it) }
            PathConfigurationComponent.PredicateType.MVC -> pathMethods.map { HttpFilterMatcherMVC(prefix, it) }
        }.sortedBy { it.config }

    open fun findMatcher(exchange: ServerWebExchange): HttpFilterMatcher<*>? {
        val uri = exchange.request.path.toString()
        val matcher = matchers.find { it.checkRequest(uri, exchange.request.method) }
        return matcher
    }

    protected open fun setMatcherAttribute(
        matcher: HttpFilterMatcher<*>?,
        exchange: ServerWebExchange,
    ) {
        if (matcher?.config?.authorities()?.haveAuth()==true) {
            exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] = matcher.config.authorities()
        } else {
            if (isSecured) {
                if (matcher != null ||matchType == PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST) {
                    exchange.attributes[PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD] =  baseAuthorities
                }
            }
        }
    }

    override fun test(t: ServerWebExchange): Boolean {
        val matcher = findMatcher(t)
        setMatcherAttribute(matcher, t)
        return when (matchType) {
            PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST -> matcher != null
            PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST -> matcher == null
        }
    }
}
