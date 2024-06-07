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

/**
 * Configuration class for R2DBC and database related configurations.
 *
 * It is annotated with `@Configuration` to indicate that it is a configuration class.
 * It is also annotated with `@EnableConfigurationProperties` to enable the usage of `R2DBCConnectionFactoryOptionsProperties` and `DataSourceProperties` configuration properties
 * .
 *
 * This class is conditionally enabled based on the value of the `spring.r2dbc.enabled` property.
 * It is imported in other configuration classes using the `@Import` annotation.
 */
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
    /**
     * Retrieves an instance of R2DBCtoJDBCUriConverter.
     *
     * @return The R2DBCtoJDBCUriConverter instance.
     */
    @Bean("r2DBCtoJDBCUriConverter")
    @ConditionalOnMissingBean(R2DBCtoJDBCUriConverter::class)
    fun getR2DBCtoJDBCUriConverter(): R2DBCtoJDBCUriConverter {
        return R2DBCtoJDBCUriConverter()
    }

    /**
     * Retrieves the FlywayR2DBCConfigBeanPostProcessor bean for configuring Flyway with R2DBC.
     *
     * @param converter The R2DBCtoJDBCUriConverter used to convert the R2DBC URL to a JDBC URL.
     * @return The FlywayR2DBCConfigBeanPostProcessor instance.
     */
    @Bean("flywayR2DBCConfig")
    @ConditionalOnMissingBean(FlywayR2DBCConfigBeanPostProcessor::class)
    fun getFlywayR2DBCConfig(converter: R2DBCtoJDBCUriConverter): FlywayR2DBCConfigBeanPostProcessor {
        return FlywayR2DBCConfigBeanPostProcessor(converter)
    }

    /**
     * Creates a Flyway migration strategy that executes the migration and callbacks.
     *
     * @param callbacks The list of callbacks to be executed after the migration.
     * @return The FlywayMigrationStrategy instance.
     */
    @Bean
    fun flywayMigrationStrategy(callbacks: List<FlywayMigrateCallback>): FlywayMigrationStrategy {
        return FlywayMigrationStrategy { flyway ->
            flyway.migrate()
            callbacks.forEach { action -> action.call() }
        }
    }

    /**
     * Retrieves the `DataSourceR2DBCConfigBeanPostProcessor` bean for configuring the R2DBC data source.
     *
     * @param converter The `R2DBCtoJDBCUriConverter` used to convert the R2DBC URL to a JDBC URL.
     * @return The `DataSourceR2DBCConfigBeanPostProcessor` instance.
     */
    @Bean("dataSourceR2DBCConfig")
    @ConditionalOnMissingBean(DataSourceR2DBCConfigBeanPostProcessor::class)
    fun getDataSourceR2DBCConfig(converter: R2DBCtoJDBCUriConverter): DataSourceR2DBCConfigBeanPostProcessor {
        return DataSourceR2DBCConfigBeanPostProcessor(converter)
    }

    /**
     * Retrieves the R2DBCConnectionFactoryOptionsBuilderCustomizer instance by customizing the ConnectionFactoryOptions.Builder with the given R2DBC connection factory options map
     * .
     *
     * @param props The R2DBCConnectionFactoryOptionsProperties object containing the R2DBC connection factory options.
     * @return The R2DBCConnectionFactoryOptionsBuilderCustomizer instance.
     */
    @Bean
    fun getR2DBCConnectionFactoryOptionsBuilderCustomizer(props: R2DBCConnectionFactoryOptionsProperties): R2DBCConnectionFactoryOptionsBuilderCustomizer {
        return R2DBCConnectionFactoryOptionsBuilderCustomizer(props.options.filter { it.value != null } as Map<String, String>)
    }

    companion object {
        /**
         * Regular expression pattern representing the limitations for SQL names.
         *
         * The SQL_NAME_LIMITATIONS_REGEX pattern is used to validate SQL names against the following limitations:
         * 1. The name must start with a lowercase letter (a-z).
         * 2. The name can contain lowercase letters (a-z), underscores (_), numbers (0-9), and parenthesis and periods ().
         * 3. The name must not start with "pg_" as it is reserved for PostgreSQL system object names.
         * 4. The name must not exceed 32 characters in length.
         *
         * Example usage:
         *
         * ```
         * val name = "my_table"
         * if (SQL_NAME_LIMITATIONS_REGEX.matches(name)) {
         *    */
        val SQL_NAME_LIMITATIONS_REGEX = Regex("^(?!pg_)[a-z][_a-z0-9).]{0,32}\$")
    }
}
