package com.alcosi.nft.apigateway.config.db.r2dbc

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.HikariCheckpointRestoreLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import javax.sql.DataSource

@Configuration
@ConditionalOnClass(DataSource::class, EmbeddedDatabaseType::class)
@EnableConfigurationProperties(DataSourceProperties::class)
@Import(
    DataSourcePoolMetadataProvidersConfiguration::class,
    DataSourceAutoConfiguration::class
)
class R2DBCJDBCDataSourceAutoConfiguration: DataSourceAutoConfiguration() {

        @Bean
        @ConditionalOnClass(HikariDataSource::class)
        @ConditionalOnMissingBean
        fun hikariCheckpointRestoreLifecycle(dataSource: DataSource?): HikariCheckpointRestoreLifecycle {
            return HikariCheckpointRestoreLifecycle(dataSource)
        }
}