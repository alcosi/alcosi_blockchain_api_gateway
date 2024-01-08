package com.alcosi.nft.apigateway.service.statistic

import com.alcosi.lib.executors.SchedulerTimer
import com.alcosi.nft.apigateway.config.db.r2dbc.R2DBCDBConfig.Companion.SQL_NAME_LIMITATIONS_REGEX
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.logging.Level
@Configuration
open class StatisticPartitionsDBInitializerConfig(
) {
    @Bean
    fun getStatisticPartitionsDBInitializer(@Value("\${spring.r2dbc.partitions.scheduler.delay:1d}") schedulerDelay:Duration,
                                            databaseClient: DatabaseClient,
                                            r2dbcProperties: R2dbcProperties

    ):StatisticPartitionsDBInitializer{
        return StatisticPartitionsDBInitializer(databaseClient, r2dbcProperties.properties["schema"] !!,schedulerDelay)
    }
}