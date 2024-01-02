package com.alcosi.nft.apigateway.service.predicate

import com.alcosi.nft.apigateway.config.FilterMatchConfig
import com.alcosi.nft.apigateway.config.PathConfig
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.web.server.ServerWebExchange
import java.util.function.Predicate

open class SecurityConfigGatewayPredicate(
    open val delegate: FilterMatchPredicate,
    open val securityConfig: PathConfig.SecurityRouteConfig,
    open val attrSecurityConfigFiled: String,
) : Logging, Predicate<ServerWebExchange> {

    override fun test(t: ServerWebExchange): Boolean {
        val haveToPass = delegate.test(t)
        setConfig(haveToPass, t)
        return haveToPass
    }

    protected open fun setConfig(haveToPass: Boolean, t: ServerWebExchange) {
        if (haveToPass) {
            t.attributes[attrSecurityConfigFiled] = securityConfig
        }
    }
}