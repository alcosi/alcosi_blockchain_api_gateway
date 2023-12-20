package com.alcosi.nft.apigateway.service.predicate.matcher

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO

class HttpFilterMatcherRegex(prefix: String, config: FilterMatchConfigDTO) : HttpFilterMatcher<Regex>(prefix, config) {
    override val predicateType: PathConfigurationComponent.PredicateType = PathConfigurationComponent.PredicateType.REGEX

    override fun checkUri(uri: String): Boolean {
        return matcher.matches(uri)
    }

    override fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): Regex {
        return "$prefix${config.path}".replace("/", "\\/").toRegex()
    }
}
