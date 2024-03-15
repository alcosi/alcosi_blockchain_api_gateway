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

package com.alcosi.nft.apigateway.config.db.r2dbc

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableConfigurationProperties(R2DBCConnectionFactoryOptionsProperties::class, DataSourceProperties::class)
@ConditionalOnProperty(prefix = "spring.r2dbc", name = ["enabled"], matchIfMissing = false, havingValue = "true")
@Import(
    value = [
        DataSourcePoolMetadataProvidersConfiguration::class,
        DataSourceAutoConfiguration::class, R2dbcAutoConfiguration::class, TransactionAutoConfiguration::class, R2dbcTransactionManagerAutoConfiguration::class, DataSourceAutoConfiguration::class, FlywayAutoConfiguration::class,
    ],
)
class R2DBCDBConfig : DataSourceAutoConfiguration() {
    @Bean("r2DBCtoJDBCUriConverter")
    @ConditionalOnMissingBean(R2DBCtoJDBCUriConverter::class)
    fun getR2DBCtoJDBCUriConverter(): R2DBCtoJDBCUriConverter {
        return R2DBCtoJDBCUriConverter()
    }

    @Bean("flywayR2DBCConfig")
    @ConditionalOnMissingBean(FlywayR2DBCConfigBeanPostProcessor::class)
    fun getFlywayR2DBCConfig(converter: R2DBCtoJDBCUriConverter): FlywayR2DBCConfigBeanPostProcessor {
        return FlywayR2DBCConfigBeanPostProcessor(converter)
    }

    @Bean
    fun flywayMigrationStrategy(callbacks: List<FlywayMigrateCallback>): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            flyway.migrate()
            callbacks.forEach { action -> action.call() }
        }
    }

    @Bean("dataSourceR2DBCConfig")
    @ConditionalOnMissingBean(DataSourceR2DBCConfigBeanPostProcessor::class)
    fun getDataSourceR2DBCConfig(converter: R2DBCtoJDBCUriConverter): DataSourceR2DBCConfigBeanPostProcessor {
        return DataSourceR2DBCConfigBeanPostProcessor(converter)
    }

    @Bean
    fun getR2DBCConnectionFactoryOptionsBuilderCustomizer(props: R2DBCConnectionFactoryOptionsProperties): R2DBCConnectionFactoryOptionsBuilderCustomizer {
        return R2DBCConnectionFactoryOptionsBuilderCustomizer(props.options.filter { it.value != null } as Map<String, String>)
    }

    companion object {
        val SQL_NAME_LIMITATIONS_REGEX = Regex("^(?!pg_)[a-z][_a-z0-9).]{0,32}\$")
    }
}
