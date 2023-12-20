package com.alcosi.nft.apigateway.service.predicate

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.config.path.dto.SecurityRouteConfigDTO
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

open class SecurityConfigGatewayPredicate(
    open val delegate: FilterMatchPredicate,
    open val securityConfig: SecurityRouteConfigDTO,
) : Logging, Predicate<ServerWebExchange> {
    override fun test(t: ServerWebExchange): Boolean {
        val haveToPass = delegate.test(t)
        setConfig(haveToPass, t)
        return haveToPass
    }

    protected open fun setConfig(
        haveToPass: Boolean,
        t: ServerWebExchange,
    ) {
        if (haveToPass) {
            t.attributes[PathConfigurationComponent.ATTRIBUTE_SECURITY_CONFIG_FIELD] = securityConfig
        }
    }
}
