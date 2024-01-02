package com.alcosi.nft.apigateway.service.predicate

import com.alcosi.nft.apigateway.config.FilterMatchConfig
import com.alcosi.nft.apigateway.config.PathConfig
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcher
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherMVC
import com.alcosi.nft.apigateway.service.predicate.matcher.HttpFilterMatcherRegex
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate
open class FilterMatchPredicate(
    prefix: String,
    pathMethods: List<FilterMatchConfig>,
    val type: PathConfig.PREDICATE_TYPE,
    val matchType:PredicateMatcherType,
    val attrReqAuthoritiesField:String,
    val baseAuthorities:List<String>,
    ) : Logging, Predicate<ServerWebExchange> {
   open val matchers: List<HttpFilterMatcher<*>> = when (type) {
        PathConfig.PREDICATE_TYPE.REGEX -> pathMethods.map { HttpFilterMatcherRegex(prefix, it) }
        PathConfig.PREDICATE_TYPE.MVC -> pathMethods.map { HttpFilterMatcherMVC(prefix, it) }
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
        if (matcher != null) {
            exchange.attributes[attrReqAuthoritiesField] = matcher.config.authorities()
        } else{
            if (matchType== PredicateMatcherType.MATCH_IF_NOT_CONTAINS_IN_LIST){
                exchange.attributes[attrReqAuthoritiesField] = baseAuthorities
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