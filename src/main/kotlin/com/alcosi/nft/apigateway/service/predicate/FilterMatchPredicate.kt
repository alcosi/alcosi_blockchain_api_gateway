package com.alcosi.nft.apigateway.service.predicate

import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcher
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherMVC
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherRegex
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate
open class FilterMatchPredicate(
    val isSecured:Boolean,
    prefix: String,
    pathMethods: List<FilterMatchConfigDTO>,
    val type: PathConfigurationComponent.PREDICATE_TYPE,
    val matchType:PredicateMatcherType,
    val attrReqAuthoritiesField:String,
    val baseAuthorities:List<String>?,
    ) : Logging, Predicate<ServerWebExchange> {
   open val matchers: List<HttpFilterMatcher<*>> = when (type) {
        PathConfigurationComponent.PREDICATE_TYPE.REGEX -> pathMethods.map { HttpFilterMatcherRegex(prefix, it) }
        PathConfigurationComponent.PREDICATE_TYPE.MVC -> pathMethods.map { HttpFilterMatcherMVC(prefix, it) }
    }.sortedBy { it.config }


    open fun findMatcher(exchange: ServerWebExchange): HttpFilterMatcher<*>?{
        val uri = exchange.request.path.toString();
        val matcher = matchers.find {it.checkRequest(uri,exchange.request.method)}
        return matcher
    }

    protected open fun setMatcherAttribute(
        matcher: HttpFilterMatcher<*>?,
        exchange: ServerWebExchange
    ) {
        if (isSecured) {
            if (matcher != null) {
                exchange.attributes[attrReqAuthoritiesField] = matcher.config.authorities() ?: baseAuthorities
            } else {
                if (matchType == PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST) {
                    exchange.attributes[attrReqAuthoritiesField] = baseAuthorities
                }
            }
        }
    }
    override fun test(t: ServerWebExchange): Boolean {
        val matcher = findMatcher(t)
        setMatcherAttribute(matcher, t)
        return when (matchType){
            PredicateMatcherType.MATCH_IF_CONTAINS_IN_LIST -> matcher!=null
            PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST->matcher==null
        }
    }
}