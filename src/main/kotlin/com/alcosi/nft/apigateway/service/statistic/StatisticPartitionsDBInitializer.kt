package com.alcosi.nft.apigateway.service.statistic

import com.alcosi.lib.executors.SchedulerTimer
import com.alcosi.nft.apigateway.config.db.r2dbc.R2DBCDBConfig.Companion.SQL_NAME_LIMITATIONS_REGEX
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

open class StatisticPartitionsDBInitializer(
    val databaseClient: DatabaseClient,
    val schemaName: String = "request_history",
    val schedulerDelay:Duration = Duration.ofDays(1)
) {
    data class PartitionResult(val time:LocalDateTime,val result:Boolean)
    init {
        if (!SQL_NAME_LIMITATIONS_REGEX.matches(schemaName)) {
            throw IllegalArgumentException("Wrong schema name $schemaName")
        }
    }
    open protected val sql= """
select t as time,${schemaName}.create_partition_by_interval(t,interval '1 month','${schemaName}.api_gateway_request_history'::text) as result
from unnest(ARRAY[current_timestamp::timestamp,current_timestamp::timestamp+interval '1 month',current_timestamp::timestamp+interval '2 month']) t;
"""
    protected open val scheduler = object : SchedulerTimer(schedulerDelay,"PartitionsDBInitializer", Level.INFO, Duration.ofSeconds(0)) {
        override fun startBatch() {

                val time = System.currentTimeMillis()
                val result=initPartition()
                val createdPeriods=result.filter { it.result } .map { it.time }.map {YearMonth.from(it) }
                val took=System.currentTimeMillis()-time
                logger.info("StatisticPartitionsDBInitializer took ${took}ms. Created:${createdPeriods.joinToString(";")}")
        }
    }
    open fun initPartition():List<PartitionResult> {
          return databaseClient
                .sql(sql).mapProperties(PartitionResult::class.java).all().collectList()
                .block()!!
    }
}