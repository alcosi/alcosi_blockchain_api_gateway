package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.alcosi.lib.security.PrincipalDetails
import org.apache.logging.log4j.kotlin.Logging
import reactor.core.publisher.Mono

interface Oath2UserInfoProvider : Logging {
    fun getInfo(token: String): Mono<PrincipalDetails>
}
