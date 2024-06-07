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

package com.alcosi.nft.apigateway.service.gateway

import com.alcosi.nft.apigateway.config.path.PathConfigurationComponent
import com.alcosi.nft.apigateway.service.DynamicRouteLocator
import com.alcosi.nft.apigateway.service.gateway.filter.MicroserviceGatewayFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * This class is responsible for configuring the dynamic route locator for the GatewayProxyRoutes.
 * The dynamic route locator is used to dynamically configure and manage proxy routes for the gateway.
 *
 * @Configuration annotation indicates that this class is a configuration class.
 */
@Configuration
class GatewayProxyRoutesConfig {
    /**
     * Configures the dynamic route locator for the GatewayProxyRoutes.
     *
     * @param props The PathConfigurationComponent containing the proxy configuration properties.
     * @param filtersList The list of MicroserviceGatewayFilter objects to be applied to the routes.
     * @param routesBuilder The RouteLocatorBuilder used for building the routes.
     * @return The configured DynamicRouteLocator object.
     */
    @Bean
    @ConditionalOnMissingBean(DynamicRouteLocator::class)
    fun configDynamicRouteLocator(
        props: PathConfigurationComponent,
        filtersList: List<MicroserviceGatewayFilter>,
        routesBuilder: RouteLocatorBuilder,
    ): DynamicRouteLocator {
        return DynamicRouteLocator(props.proxyConfig, filtersList, routesBuilder)
    }
}
