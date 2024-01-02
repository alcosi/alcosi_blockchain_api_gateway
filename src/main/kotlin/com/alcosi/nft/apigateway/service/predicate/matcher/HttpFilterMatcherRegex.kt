package com.alcosi.nft.apigateway.service.predicate.matcher

import com.alcosi.nft.apigateway.config.dto.FilterMatchConfigDTO
import com.alcosi.nft.apigateway.config.PathConfig

class HttpFilterMatcherRegex(prefix: String, config: FilterMatchConfigDTO) : HttpFilterMatcher<Regex>(prefix, config) {
    override val predicateType: PathConfig.PREDICATE_TYPE = PathConfig.PREDICATE_TYPE.REGEX
    override fun checkUri(uri: String): Boolean {
        return matcher.matches(uri)
    }

     override fun createMather(prefix: String, config: FilterMatchConfigDTO): Regex {
        return "$prefix${config.path}".replace("/", "\\/").toRegex()
    }

}