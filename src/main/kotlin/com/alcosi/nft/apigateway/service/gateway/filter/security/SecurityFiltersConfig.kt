package com.alcosi.nft.apigateway.service.gateway.filter.security

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.auth.service.CheckJWTService
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.eth.EthJwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.oath2.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnProperty(matchIfMissing=true, prefix = "filter.config.path.security", value= ["enabled"],havingValue = "true")
class SecurityFiltersConfig {


    @Bean
    @ConditionalOnMissingBean(JwtGatewayFilter::class)
    @ConditionalOnProperty(matchIfMissing=true, prefix = "filter.config.path.security.type", value= ["method"],havingValue = "ETH_JWT")
    fun getEthJwtGatewayFilter(
        securityGatewayFilter: SecurityGatewayFilter,
        checkJWTService: CheckJWTService,
        mappingHelper: MappingHelper
    ): JwtGatewayFilter {
        return EthJwtGatewayFilter(securityGatewayFilter, checkJWTService)
    }
    @Bean
    @ConditionalOnMissingBean(JwtGatewayFilter::class)
    @ConditionalOnProperty(matchIfMissing=false, prefix = "filter.config.path.security.type", value= ["method"],havingValue = "IDENTITY_SERVER")
    fun getOath2JwtGatewayFilter(
        securityGatewayFilter: SecurityGatewayFilter,
        getInfoService: Oath2GetUserInfoService,
        mappingHelper: MappingHelper
    ): JwtGatewayFilter {
        return Oath2GatewayFilter(securityGatewayFilter, getInfoService,mappingHelper)
    }
    @Bean
    @ConditionalOnMissingBean(Oath2GetUserInfoService::class)
    @ConditionalOnProperty(matchIfMissing=false, prefix = "filter.config.path.security.type", value= ["method"],havingValue = "IDENTITY_SERVER")
    fun getOath2GetUserInfoService(
        @Value("\${filter.config.path.security.identity_server.claim-client-id:clientId}") claimClientId: String,
        @Value("\${filter.config.path.security.identity_server.claim-type:type}") claimType: String,
        @Value("\${filter.config.path.security.identity_server.claim-authorities:authorities}") claimAuthorities: String,
        oath2GetUserInfoComponent: Oath2GetUserInfoComponent,
        oath2APIGetUserInfoComponent: Oath2APIGetUserInfoComponent
    ): Oath2GetUserInfoService {
        return Oath2GetUserInfoService(claimClientId,claimType,claimAuthorities,oath2GetUserInfoComponent, oath2APIGetUserInfoComponent)
    }
    @Bean
    @ConditionalOnMissingBean(Oath2GetUserInfoComponent::class)
    @ConditionalOnProperty(matchIfMissing=false, prefix = "filter.config.path.security.type", value= ["method"],havingValue = "IDENTITY_SERVER")
    fun getOath2GetUserInfoComponent(
        mappingHelper: MappingHelper,
        webClient: WebClient,
        objectMapper: ObjectMapper,
        @Value("\${filter.config.path.security.identity_server.uri}") idServerUri: String,
    ): Oath2GetUserInfoComponent {
        return Oath2GetUserInfoComponent(mappingHelper,webClient, objectMapper,idServerUri)
    }
    @Bean
    @ConditionalOnMissingBean(Oath2APIGetUserInfoComponent::class)
    @ConditionalOnProperty(matchIfMissing=false, prefix = "filter.config.path.security.type", value= ["method"],havingValue = "IDENTITY_SERVER")
    fun getOath2APIGetUserInfoComponent(
        mappingHelper: MappingHelper,
        webClient: WebClient,
        oath2AuthComponent: Oath2AuthComponent,
        @Value("\${filter.config.path.security.identity_server.uri}") idServerUri: String,
        @Value("\${filter.config.path.security.identity_server.api_version:2.0}") idServerXApiVersion: String,
        ): Oath2APIGetUserInfoComponent {
        return Oath2APIGetUserInfoComponent(mappingHelper,webClient, oath2AuthComponent,idServerXApiVersion,idServerUri)
    }
    @Bean
    @ConditionalOnMissingBean(Oath2AuthComponent::class)
    @ConditionalOnProperty(matchIfMissing=false, prefix = "filter.config.path.security.type", value= ["method"],havingValue = "IDENTITY_SERVER")
    fun getOath2AuthComponent(
        webClient: WebClient,
        @Value("\${filter.config.path.security.identity_server.uri}") idServerUri: String,
        @Value("\${filter.config.path.security.identity_server.client_id}") clientId: String,
        @Value("\${filter.config.path.security.identity_server.client_secret}") clientSecret: String,
        @Value("\${filter.config.path.security.identity_server.client_scopes}") clientScopes: String,
        @Value("\${filter.config.path.security.identity_server.grant_type}") grantType: String,
        @Value("\${filter.config.path.security.identity_server.password}") password: String,
        @Value("\${filter.config.path.security.identity_server.username}") username: String,
        ): Oath2AuthComponent {
        return Oath2AuthComponent(webClient, idServerUri,clientId,clientSecret,clientScopes.split(" "),grantType, username, password)
    }
    @Bean
    @ConditionalOnMissingBean(SecurityGatewayFilter::class)
    fun getSecurityGatewayFilter(
        pathConfig: PathConfigurationComponent
    ): SecurityGatewayFilter {
        return SecurityGatewayFilter(pathConfig.securityConfig.toPredicate(), PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD)
    }

}