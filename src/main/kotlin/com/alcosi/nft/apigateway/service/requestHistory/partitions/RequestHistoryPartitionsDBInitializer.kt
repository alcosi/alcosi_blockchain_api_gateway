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

package com.alcosi.nft.apigateway.service.requestHistory.partitions

import com.alcosi.nft.apigateway.config.db.r2dbc.FlywayMigrateCallback
import com.alcosi.nft.apigateway.config.db.r2dbc.R2DBCDBConfig.Companion.SQL_NAME_LIMITATIONS_REGEX
import io.github.breninsul.javatimerscheduler.registry.SchedulerType
import io.github.breninsul.javatimerscheduler.registry.TaskSchedulerRegistry
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.scheduler.Scheduler
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.logging.Level

/**
 * A class responsible for initializing partitions for a database table.
 *
 * @property databaseClient The client used to interact with the database.
 * @property flexScheduler The scheduler used for executing tasks.
 * @property schemaName The name of the schema.
 * @property monthDelta The number of months to create partitions for.
 * @property schedulerDelay The delay between each partition creation task.
 */
open class RequestHistoryPartitionsDBInitializer(
    val databaseClient: DatabaseClient,
    val flexScheduler: Scheduler,
    schemaName: String = "request_history",
    monthDelta: Int = 2,
    schedulerDelay: Duration = Duration.ofDays(1),
) : FlywayMigrateCallback, Logging {
    /**
     * Represents the result of a partition operation.
     *
     * @property time The date and time when the partition operation was performed.
     * @property result The boolean result of the partition operation.
     */
    data class PartitionResult(val time: LocalDateTime, val result: Boolean)

    /**
     *  Validate schema name to avoid SQL injection
     */
    init {
        if (!SQL_NAME_LIMITATIONS_REGEX.matches(schemaName)) {
            throw IllegalArgumentException("Wrong schema name $schemaName")
        }
    }

    /**
     * Creates partitions for the database table.
     *
     * This method initializes partitions for the database table by calling the internal method `initPartition()`.
     * It then filters the result for successful partitions and extracts the created periods by mapping them to `YearMonth` instances.
     *
     * The total time taken for creating the partitions is recorded and logged using the `logger` object.
     * If any error occurs during the process, it is caught and logged as an error.
     *
     * @throws Throwable if an error occurs during partition creation.
     */
    override fun call() {
        createPartitions()
    }

    /**
     * The SQL query to create partitions with interval on the "api_gateway_request_history" table.
     *
     * This query uses a common table expression (CTE) to generate a series of months based on the given monthDelta value.
     * It then creates intervals based on the generated series, and calculates corresponding timestamps using the current timestamp.
     * The timestamps are truncated to the month level using the date_trunc function, and stored in the "truncated" CTE.
     * Finally, the query selects the truncated timestamps as "time" and calls the create_partition_by_interval function
     * to create partitions on the "api_gateway_request_history" table based on the truncated timestamps.
     * The result of the create_partition_by_interval function is also selected as "result" in the final result set.
     *
     * @property sql The SQL query to create partitions with interval on the "api_gateway_request_history" table.
     */
    protected open val sql = """
with series as (select s::text||' month' as ser from generate_series(0,$monthDelta) s),
intervals as(
    select s.ser::interval as inter from series s
),
times as(
    select current_timestamp+i.inter as time from intervals i
),
truncated as(
         select date_trunc('month',t.time)::timestamp as trunc from times t
)
select t.trunc as time ,$schemaName.create_partition_by_interval(t.trunc,'1 month'::interval ,'api_gateway_request_history'::text,'$schemaName'::text ) as result from truncated t;

"""

    /**
     * This is the initialization block for PartitionsDBInitializer. It
     * schedules a task that is periodically run, which upon execution,
     * initializes the database partitions and logs the time taken to do so as
     * well as the periods created. If an error occurs during this process, it
     * will also be logged.
     */
    init {
        TaskSchedulerRegistry.registerTypeTask(SchedulerType.VIRTUAL_WAIT, "PartitionsDBInitializer", schedulerDelay, loggingLevel = Level.INFO, runnable = Runnable {
            createPartitions()
        })
    }

    /**
     * Creates partitions for the database table.
     *
     * This method initializes partitions for the database table by calling the internal method `initPartition()`. It then filters the result for successful partitions and extracts
     *  the created periods by mapping them to `YearMonth` instances.
     *
     * The total time taken for creating the partitions is recorded and logged using the `logger` object. If any error occurs during the process, it is caught and logged as an error
     * .
     *
     * @throws Throwable if an error occurs during partition creation.
     */
    protected open fun createPartitions() {
        try {
            val time = System.currentTimeMillis()
            val result = initPartition()
            val createdPeriods = result.filter { it.result }.map { it.time }.map { YearMonth.from(it) }
            val took = System.currentTimeMillis() - time
            logger.info("StatisticPartitionsDBInitializer took ${took}ms. Created:${createdPeriods.joinToString(";")}",)
        } catch (t: Throwable) {
            logger.error("Error StatisticPartitionsDBInitializer", t)
        }
    }

    /**
     * Initializes the partition by executing the SQL query and mapping the results to a list of PartitionResult objects.
     *
     * @return a list of PartitionResult objects representing the results of the query
     */
    open fun initPartition(): List<PartitionResult> {
        return databaseClient
            .sql(sql)
            .mapProperties(PartitionResult::class.java).all().collectList()
            .subscribeOn(flexScheduler)
            .block()!!
    }
}
