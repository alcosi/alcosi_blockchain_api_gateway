package com.alcosi.nft.apigateway.config.path

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "filter.config.path")
class PathConfigurationProperties {
    var validation: Map<String, Any?> = mapOf()
    var security: Map<String, Any?> = mapOf()
    var proxy: Map<String, String?> = mapOf()
}
