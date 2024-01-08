package com.alcosi.nft.apigateway.config.db.r2dbc

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.r2dbc")
data class R2DBCConnectionFactoryOptionsProperties(var options: Map<String,String?> = mapOf())