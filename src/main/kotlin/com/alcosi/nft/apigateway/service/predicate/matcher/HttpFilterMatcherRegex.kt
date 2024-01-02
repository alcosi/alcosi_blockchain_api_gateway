package com.alcosi.nft.apigateway.service.predicate.matcher

import com.alcosi.nft.apigateway.config.FilterMatchConfig
import com.alcosi.nft.apigateway.config.PathConfig

class HttpFilterMatcherRegex(prefix: String, config: FilterMatchConfig) : HttpFilterMatcher<Regex>(prefix, config) {
    override val predicateType: PathConfig.PREDICATE_TYPE = PathConfig.PREDICATE_TYPE.REGEX
    override fun checkUri(uri: String): Boolean {
        return matcher.matches(uri)
    }

     override fun createMather(prefix: String, config: FilterMatchConfig): Regex {
        return "$prefix${config.path}".replace("/", "\\/").toRegex()
    }

}