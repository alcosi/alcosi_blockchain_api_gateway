/*
 * Copyright (c) 2023 Alcosi Group Ltd. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alcosi.nft.apigateway.service.requestHistory

import com.alcosi.nft.apigateway.config.db.r2dbc.R2DBCConnectionFactoryOptionsProperties
import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.gateway.filter.LoggingFilter
import com.alcosi.nft.apigateway.service.gateway.filter.security.JwtGatewayFilter
import com.alcosi.nft.apigateway.service.requestHistory.filter.RequestHistoryGatewayFilterRq
import com.alcosi.nft.apigateway.service.requestHistory.filter.RequestHistoryGatewayFilterSecurity
import com.alcosi.nft.apigateway.service.requestHistory.partitions.RequestHistoryPartitionsDBInitializer
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.breninsul.namedlimitedvirtualthreadexecutor.service.VirtualNamedLimitedExecutorService
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

/**
 * This class provides the configuration for the RequestHistory feature.
 *
 * The RequestHistoryConfig class is responsible for creating and configuring the necessary beans to enable the RequestHistory feature.
 * It ensures that the RequestHistory feature is enabled based on the "spring.r2dbc.enabled" property.
 * It also creates and configures beans for various components required for request history functionality.
 *
 * @constructor Creates an instance of RequestHistoryConfig.
 */
@Configuration
@ConditionalOnBean(DatabaseClient::class)
@EnableConfigurationProperties(R2DBCConnectionFactoryOptionsProperties::class)
@ConditionalOnProperty(prefix = "spring.r2dbc", name = ["enabled"], matchIfMissing = false, havingValue = "true")
open class RequestHistoryConfig() {
    /**
     * Returns a scheduler for saving request history to the database.
     *
     * @param props The configuration properties for R2DBC connection factory options.
     * @return A Scheduler object for saving request history.
     */
    @Bean("RequestHistoryDBSaveFlexScheduler")
    @ConditionalOnMissingBean(name = ["RequestHistoryDBSaveFlexScheduler"])
    fun getRequestHistoryDBSaveFlexScheduler(props: R2DBCConnectionFactoryOptionsProperties): Scheduler {
       return Schedulers.fromExecutorService(VirtualNamedLimitedExecutorService("RequestHistory",props.maxTasks,))
    }
    /**
     * Creates a new instance of `RequestHistoryGatewayFilterRq`.
     *
     * @param requestHistoryDBService The instance of `RequestHistoryDBService` used to save request history.
     * @param props The `R2DBCConnectionFactoryOptionsProperties` object containing connection options for R2DBC.
     * @return The created `RequestHistoryGatewayFilterRq` object.
     */
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
    /**
     * Creates a new instance of RequestHistoryGatewayFilterSecurity.
     * This method is annotated with @Bean and @ConditionalOnMissingBean, and it is used to create an instance of RequestHistoryGatewayFilterSecurity when it is not already present
     *  in the application context.
     *
     * @param requestHistoryDBService The service used to interact with the request history database.
     * @param jwtFilters The list of JwtGatewayFilter instances.
     * @return The created RequestHistoryGatewayFilterSecurity instance.
     */
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
    /**
     * Creates an instance of RequestHistoryDBComponent.
     *
     * @param props The R2dbcProperties object that provides R2DBC related configuration properties.
     * @param dbClient The DatabaseClient object used for interacting with the database.
     * @param mappingHelper The ObjectMapper object used for serialization and deserialization of data.
     * @param flexScheduler The Scheduler object used for scheduling asynchronous tasks.
     * @return The created RequestHistoryDBComponent instance.
     */
    @Bean
    @ConditionalOnMissingBean(RequestHistoryDBComponent::class)
    fun getRequestHistoryDBComponent(
        props: R2dbcProperties,
        dbClient: DatabaseClient,
        mappingHelper: ObjectMapper,
        @Qualifier("RequestHistoryDBSaveFlexScheduler") flexScheduler: Scheduler,
    ): RequestHistoryDBComponent {
        return RequestHistoryDBComponent(
            flexScheduler,
            dbClient,
            mappingHelper,
            props.properties["schema"] ?: "request_history",
        )
    }
    /**
     * Create an instance of RequestHistoryDBService.
     *
     * @param component The RequestHistoryDBComponent instance.
     * @param dbClient The DatabaseClient instance.
     * @param props The R2DBCConnectionFactoryOptionsProperties instance.
     * @return The created RequestHistoryDBService instance.
     */
    @Bean
    @ConditionalOnMissingBean(RequestHistoryDBService::class)
    fun getRequestHistoryDBService(
        component: RequestHistoryDBComponent,
        dbClient: DatabaseClient,
        props: R2DBCConnectionFactoryOptionsProperties,
    ): RequestHistoryDBService {
        return RequestHistoryDBService(
            component,
            props.requestHistoryIpHeader,
            props.requestHistoryMaskHeaders
        )
    }
    /**
     * Retrieves the initialized RequestHistoryPartitionsDBInitializer bean.
     *
     * @param props The R2DBCConnectionFactoryOptionsProperties object containing the configuration properties for R2DBC connection factory options.
     * @param databaseClient The DatabaseClient object used for database operations.
     * @param r2dbcProperties The R2dbcProperties object containing the configuration properties for R2DBC.
     * @param flexScheduler The Scheduler object used for scheduling flex tasks related to request history.
     * @return The initialized RequestHistoryPartitionsDBInitializer bean.
     */
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
    /**
     * Returns an instance of RequestHistoryExceptionHandler.
     *
     * @param requestHistoryDBService The RequestHistoryDBService used for saving request history information.
     * @param props The SpringCloudGatewayLoggingProperties used for logging configuration.
     * @param errorAttributes The ErrorAttributes used for handling errors.
     * @param webProperties The WebProperties used for web configuration.
     * @param viewResolvers The ObjectProvider<ViewResolver> used for resolving views.
     * @param serverCodecConfigurer The ServerCodecConfigurer used for codec configuration.
     * @param applicationContext The AnnotationConfigReactiveWebServerApplicationContext used for application context.
     * @param serverProperties The ServerProperties used for server configuration.
     * @param utils The SpringCloudGatewayLoggingUtils used for logging utility functions.
     * @return An instance of RequestHistoryExceptionHandler.
     */
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
        utils: LoggingFilter.Utils,
    ): RequestHistoryExceptionHandler {
        val handler = RequestHistoryExceptionHandler(requestHistoryDBService, PathConfigurationComponent.ATTRIBUTES_REQUEST_HISTORY_INFO, props.addIdHeader, utils, errorAttributes, webProperties.resources, serverProperties.error, applicationContext)
        handler.setViewResolvers(viewResolvers.orderedStream().toList())
        handler.setMessageWriters(serverCodecConfigurer.writers)
        handler.setMessageReaders(serverCodecConfigurer.readers)
        return handler
    }
}
