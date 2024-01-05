package com.alcosi.nft.apigateway.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component


@Component("SPELUtils")
@ConditionalOnMissingBean(SPELUtils::class)
open class SPELUtils{
        fun r2dbcToJdbcUri(value: String?): String? {
            return value?.replace(":pool", "")?.replace("r2dbc", "jdbc")
        }
}
