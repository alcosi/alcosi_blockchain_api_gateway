package com.alcosi.nft.apigateway.service.gateway.filter.security

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(matchIfMissing = true, prefix = "filter.config.path.security", value = ["enabled"], havingValue = "true")
class SecurityFiltersConfig {
    @Bean
    @ConditionalOnMissingBean(SecurityGatewayFilter::class)
    fun getSecurityGatewayFilter(pathConfig: PathConfigurationComponent): SecurityGatewayFilter {
        return SecurityGatewayFilter(pathConfig.securityConfig.toPredicate(), PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD)
    }
}
