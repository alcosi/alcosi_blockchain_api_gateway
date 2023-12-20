package com.alcosi.nft.apigateway.service.gateway.filter.security.eth

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.auth.service.CheckJWTService
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityFiltersConfig
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnBean(SecurityFiltersConfig::class)
@ConditionalOnProperty(matchIfMissing = true, prefix = "filter.config.path.security.type", value = ["method"], havingValue = "ETH_JWT")
class EthJwtFiltersConfig {
    @Bean
    @ConditionalOnMissingBean(JwtGatewayFilter::class)
    fun getEthJwtGatewayFilter(
        securityGatewayFilter: SecurityGatewayFilter,
        checkJWTService: CheckJWTService,
        mappingHelper: MappingHelper,
    ): JwtGatewayFilter {
        return EthJwtGatewayFilter(securityGatewayFilter, checkJWTService)
    }
}
