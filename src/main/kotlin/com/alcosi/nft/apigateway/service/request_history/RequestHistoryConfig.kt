package com.alcosi.nft.apigateway.service.request_history

import com.alcosi.lib.object_mapper.MappingHelper
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.SecurityGatewayFilter
import com.alcosi.nft.apigateway.service.request_history.filter.RequestHistoryGatewayFilterRq
import com.alcosi.nft.apigateway.service.request_history.filter.RequestHistoryGatewayFilterSecurity
import com.alcosi.nft.apigateway.service.request_history.partitions.RequestHistoryPartitionsDBInitializer
import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingErrorWebExceptionHandler
import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingProperties
import com.github.breninsul.webfluxlogging.cloud.SpringCloudGatewayLoggingUtils
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.WebProperties
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
import java.time.Duration

@Configuration
@ConditionalOnBean(DatabaseClient::class)
@ConditionalOnProperty(prefix = "spring.r2dbc", name = ["url"], matchIfMissing = false)
open class RequestHistoryConfig() {
    @Bean("RequestHistoryDBSaveFlexScheduler")
    @ConditionalOnMissingBean(name = ["RequestHistoryDBSaveFlexScheduler"])
    fun getRequestHistoryDBSaveFlexScheduler(
        @Value("\${spring.r2dbc.threads:\${spring.r2dbc.pool.max-size}}")
        threads: Int,
        @Value("\${spring.r2dbc.max-tasks:200000}")
        maxTasks: Int
    ): Scheduler {
        return Schedulers.newBoundedElastic(threads, maxTasks, "RequestHistory")
    }

    @Bean
    @ConditionalOnMissingBean(RequestHistoryGatewayFilterRq::class)
    fun getRequestHistoryGatewayFilterRq(
        requestHistoryDBService: RequestHistoryDBService,
        @Value("\${spring.r2dbc.request-history-filter.rq.order:-2147483648}") order: Int,
    ): RequestHistoryGatewayFilterRq {
        return RequestHistoryGatewayFilterRq(
            requestHistoryDBService,
            order
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
            requestHistoryDBService, order
        )
    }

    @Bean
    @ConditionalOnMissingBean(RequestHistoryDBComponent::class)
    fun getRequestHistoryDBComponent(
        props: R2dbcProperties,
        dbClient: DatabaseClient,
        mappingHelper: MappingHelper,
        @Qualifier("RequestHistoryDBSaveFlexScheduler") flexScheduler: Scheduler
    ): RequestHistoryDBComponent {
        return RequestHistoryDBComponent(
            flexScheduler,
            dbClient,
            mappingHelper,
            props.properties["schema"] ?: "request_history"
        )
    }

    @Bean
    @ConditionalOnMissingBean(RequestHistoryDBService::class)
    fun getRequestHistoryDBService(
        component: RequestHistoryDBComponent,
        dbClient: DatabaseClient,
        @Value("\${spring.r2dbc.request-history-filter.rq.ip-header:x-real-ip}") ipHeader: String,
    ): RequestHistoryDBService {
        return RequestHistoryDBService(
            component,
            ipHeader,
            PathConfigurationComponent.ATTRIBUTES_REQUEST_TIME,
            PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO,
            PathConfigurationComponent.ATTRIBUTE_PROXY_CONFIG_FIELD,
            PathConfigurationComponent.ATTRIBUTE_SECURITY_CONFIG_FIELD,
            PathConfigurationComponent.ATTRIBUTE_REQ_AUTHORITIES_FIELD,
            SecurityGatewayFilter.SECURITY_CLIENT_ATTRIBUTE,
        )
    }

    @Bean
    fun getStatisticPartitionsDBInitializer(
        @Value("\${spring.r2dbc.partitions.scheduler.delay:1d}") schedulerDelay: Duration,
        @Value("\${spring.r2dbc.partitions.scheduler.month-delta:2}") monthDelta: Int,
        databaseClient: DatabaseClient,
        r2dbcProperties: R2dbcProperties,
        @Qualifier("RequestHistoryDBSaveFlexScheduler") flexScheduler: Scheduler

    ): RequestHistoryPartitionsDBInitializer {
        return RequestHistoryPartitionsDBInitializer(
            databaseClient,
            flexScheduler,
            r2dbcProperties.properties["schema"]!!,
            monthDelta,
            schedulerDelay,
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
        val handler= RequestHistoryExceptionHandler(requestHistoryDBService,PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO,props.addIdHeader,utils,errorAttributes,webProperties.resources,serverProperties.error,applicationContext)
        handler.setViewResolvers(viewResolvers.orderedStream().toList())
        handler.setMessageWriters(serverCodecConfigurer.writers)
        handler.setMessageReaders(serverCodecConfigurer.readers)
        return handler
    }
}