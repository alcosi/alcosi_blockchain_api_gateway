package com.alcosi.nft.apigateway.service.predicate.matcher

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.FilterMatchConfigDTO
import org.springframework.http.HttpMethod

abstract class HttpFilterMatcher<T>(
    val prefix: String,
    val config: FilterMatchConfigDTO,
) {
    val matcher: T by lazy { createMather(prefix, config) }
    abstract val predicateType: PathConfigurationComponent.PredicateType
    val methods: List<HttpMethod> = config.methods

    abstract fun createMather(
        prefix: String,
        config: FilterMatchConfigDTO,
    ): T

    open fun checkRequest(
        uri: String,
        method: HttpMethod,
    ): Boolean {
        val methodMatches = checkMethod(method)
        if (!methodMatches) {
            return false
        }
        val pathMatches = checkUri(uri)
        return pathMatches
    }

    open fun checkMethod(method: HttpMethod): Boolean {
        return methods.contains(method)
    }

    abstract fun checkUri(uri: String): Boolean
}
