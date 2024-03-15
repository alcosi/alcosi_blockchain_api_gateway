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

import com.alcosi.lib.executors.SchedulerTimer
import com.alcosi.nft.apigateway.config.db.r2dbc.FlywayMigrateCallback
import com.alcosi.nft.apigateway.config.db.r2dbc.R2DBCDBConfig.Companion.SQL_NAME_LIMITATIONS_REGEX
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.scheduler.Scheduler
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.logging.Level

open class RequestHistoryPartitionsDBInitializer(
    val databaseClient: DatabaseClient,
    val flexScheduler: Scheduler,
    schemaName: String = "request_history",
    monthDelta: Int = 2,
    val schedulerDelay: Duration = Duration.ofDays(1),
) : FlywayMigrateCallback {
    data class PartitionResult(val time: LocalDateTime, val result: Boolean)

    init {
        if (!SQL_NAME_LIMITATIONS_REGEX.matches(schemaName)) {
            throw IllegalArgumentException("Wrong schema name $schemaName")
        }
    }

    override fun call() {
        scheduler.startBatch()
    }

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
    protected open val scheduler =
        object : SchedulerTimer(schedulerDelay, "PartitionsDBInitializer", Level.INFO) {
            override fun startBatch() {
                try {
                    val time = System.currentTimeMillis()
                    val result = initPartition()
                    val createdPeriods = result.filter { it.result }.map { it.time }.map { YearMonth.from(it) }
                    val took = System.currentTimeMillis() - time
                    logger.info(
                        "StatisticPartitionsDBInitializer took ${took}ms. Created:${
                            createdPeriods.joinToString(
                                ";",
                            )
                        }",
                    )
                } catch (t: Throwable) {
                    logger.log(Level.SEVERE, "Error StatisticPartitionsDBInitializer", t)
                }
            }
        }

    open fun initPartition(): List<PartitionResult> {
        return databaseClient
            .sql(sql)
            .mapProperties(PartitionResult::class.java).all().collectList()
            .subscribeOn(flexScheduler)
            .block()!!
    }
}
