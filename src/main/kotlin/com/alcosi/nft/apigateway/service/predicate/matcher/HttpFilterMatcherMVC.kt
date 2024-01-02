package com.alcosi.nft.apigateway.service.predicate.matcher

import com.alcosi.nft.apigateway.config.FilterMatchConfig
import com.alcosi.nft.apigateway.config.PathConfig
import org.springframework.http.server.PathContainer
import org.springframework.web.util.pattern.PathPattern
import org.springframework.web.util.pattern.PathPatternParser

class HttpFilterMatcherMVC(prefix: String, config: FilterMatchConfig) : HttpFilterMatcher<PathPattern>(prefix, config) {
    override val predicateType: PathConfig.PREDICATE_TYPE = PathConfig.PREDICATE_TYPE.MVC
    override fun checkUri(uri: String): Boolean {
        return matcher.matches(PathContainer.parsePath(uri))
    }

    override fun createMather(prefix: String, config: FilterMatchConfig): PathPattern {
        return pathPatternParser.parse("$prefix${config.path}")
    }
    companion object {
        val pathPatternParser = PathPatternParser()
    }
}