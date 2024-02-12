package com.alcosi.nft.apigateway.service.requestHistory

import com.alcosi.lib.objectMapper.MappingHelper
import com.alcosi.nft.apigateway.config.db.r2dbc.R2DBCConnectionFactoryOptionsProperties
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.requestHistory.filter.RequestHistoryGatewayFilterRq
import com.alcosi.nft.apigateway.service.requestHistory.filter.RequestHistoryGatewayFilterSecurity
import com.alcosi.nft.apigateway.service.requestHistory.partitions.RequestHistoryPartitionsDBInitializer
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingProperties
import io.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.web.reactive.result.view.ViewResolver
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@Configuration
@ConditionalOnBean(DatabaseClient::class)
@EnableConfigurationProperties(R2DBCConnectionFactoryOptionsProperties::class)
@ConditionalOnProperty(prefix = "spring.r2dbc", name = ["enabled"], matchIfMissing = false, havingValue = "true")
open class RequestHistoryConfig() {
    @Bean("RequestHistoryDBSaveFlexScheduler")
    @ConditionalOnMissingBean(name = ["RequestHistoryDBSaveFlexScheduler"])
    fun getRequestHistoryDBSaveFlexScheduler(props: R2DBCConnectionFactoryOptionsProperties): Scheduler {
        return Schedulers.newBoundedElastic(props.threads, props.maxTasks, "RequestHistory")
    }

    @Bean
    @ConditionalOnMissingBean(RequestHistoryGatewayFilterRq::class)
    fun getRequestHistoryGatewayFilterRq(
        requestHistoryDBService: RequestHistoryDBService,
        props: R2DBCConnectionFactoryOptionsProperties,
    ): RequestHistoryGatewayFilterRq {
        return RequestHistoryGatewayFilterRq(
            requestHistoryDBService,
            props.requestHistoryFilterOrder,
        )
    }

    @Bean
    @ConditionalOnMissingBean(RequestHistoryGatewayFilterSecurity::class)
    fun getRequestHistoryGatewayFilterSecurity(
        requestHistoryDBService: RequestHistoryDBService,
        jwtFilters: List<JwtGatewayFilter>,
    ): RequestHistoryGatewayFilterSecurity {
        val order = jwtFilters.maxOfOrNull { it.order + 1 } ?: (JwtGatewayFilter.JWT_LOG_ORDER + 1)
        return RequestHistoryGatewayFilterSecurity(
            requestHistoryDBService,
            order,
        )
    }

    @Bean
    @ConditionalOnMissingBean(RequestHistoryDBComponent::class)
    fun getRequestHistoryDBComponent(
        props: R2dbcProperties,
        dbClient: DatabaseClient,
        mappingHelper: MappingHelper,
        @Qualifier("RequestHistoryDBSaveFlexScheduler") flexScheduler: Scheduler,
    ): RequestHistoryDBComponent {
        return RequestHistoryDBComponent(
            flexScheduler,
            dbClient,
            mappingHelper,
            props.properties["schema"] ?: "request_history",
        )
    }

    @Bean
    @ConditionalOnMissingBean(RequestHistoryDBService::class)
    fun getRequestHistoryDBService(
        component: RequestHistoryDBComponent,
        dbClient: DatabaseClient,
        props: R2DBCConnectionFactoryOptionsProperties,
    ): RequestHistoryDBService {
        return RequestHistoryDBService(
            component,
            props.requestHistoryIpHeader
        )
    }

    @Bean
    fun getStatisticPartitionsDBInitializer(
        props: R2DBCConnectionFactoryOptionsProperties,
        databaseClient: DatabaseClient,
        r2dbcProperties: R2dbcProperties,
        @Qualifier("RequestHistoryDBSaveFlexScheduler") flexScheduler: Scheduler,
    ): RequestHistoryPartitionsDBInitializer {
        return RequestHistoryPartitionsDBInitializer(
            databaseClient,
            flexScheduler,
            r2dbcProperties.properties["schema"]!!,
            props.partitionsInitMonthDelta,
            props.partitionsInitSchedulerDelay,
        )
    }

    @Bean
    @Order(-1)
    @Primary
    @ConditionalOnMissingBean(RequestHistoryExceptionHandler::class)
    fun getRequestHistoryExceptionHandler(
        requestHistoryDBService: RequestHistoryDBService,
        props: SpringCloudGatewayLoggingProperties,
        errorAttributes: ErrorAttributes,
        webProperties: WebProperties,
        viewResolvers: ObjectProvider<ViewResolver>,
        serverCodecConfigurer: ServerCodecConfigurer,
        applicationContext: AnnotationConfigReactiveWebServerApplicationContext,
        serverProperties: ServerProperties,
        utils: SpringCloudGatewayLoggingUtils,
    ): RequestHistoryExceptionHandler {
        val handler = RequestHistoryExceptionHandler(requestHistoryDBService, PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO, props.addIdHeader, utils, errorAttributes, webProperties.resources, serverProperties.error, applicationContext)
        handler.setViewResolvers(viewResolvers.orderedStream().toList())
        handler.setMessageWriters(serverCodecConfigurer.writers)
        handler.setMessageReaders(serverCodecConfigurer.readers)
        return handler
    }
}
