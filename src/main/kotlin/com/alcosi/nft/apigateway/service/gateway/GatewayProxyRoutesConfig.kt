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

@Configuration
class GatewayProxyRoutesConfig {
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
