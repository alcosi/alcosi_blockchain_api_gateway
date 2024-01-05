package com.alcosi.nft.apigateway.service.predicate.matcher

import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent

class HttpFilterMatcherRegex(prefix: String, config: FilterMatchConfigDTO) : HttpFilterMatcher<Regex>(prefix, config) {
    override val predicateType: PathConfigurationComponent.PREDICATE_TYPE = PathConfigurationComponent.PREDICATE_TYPE.REGEX
    override fun checkUri(uri: String): Boolean {
        return matcher.matches(uri)
    }

     override fun createMather(prefix: String, config: FilterMatchConfigDTO): Regex {
        return "$prefix${config.path}".replace("/", "\\/").toRegex()
    }

}