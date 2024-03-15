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

import com.alcosi.lib.objectMapper.MappingHelper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

open class RequestHistoryDBComponent(
    protected val flexScheduler: Scheduler,
    protected val dbClient: DatabaseClient,
    protected val mappingHelper: MappingHelper,
    schemaName: String = "request_history",
) {
    protected open val sqlSaveRequest = """insert into $schemaName.api_gateway_request_history(rq_id,rq_headers, ip, uri, method,rq_size,  rq_time,routed_service,  matched_route_details)
                 values (:rq_id,:rq_headers::jsonb,:ip,:uri,:method::$schemaName.http_method_type,:rq_size,:rq_time,:routed_service,:matched_route_details::jsonb)
                 returning id"""

    open fun saveRequest(
        rqId: String,
        rqHeaders: Map<String,String?>,
        ip: String?,
        uri: String,
        method: HttpMethod,
        rqSize: Long,
        rqTime: LocalDateTime,
        routedService: String?,
        routeDetails: Any?,
    ): Mono<Long> {
        return dbClient.sql(sqlSaveRequest)
            .bindValueOrNull("rq_id", rqId, String::class.java)
            .bindValueOrNull("rq_headers", mappingHelper.serialize(rqHeaders), String::class.java)
            .bindValueOrNull("ip", ip, String::class.java)
            .bindValueOrNull("uri", uri, String::class.java)
            .bindValueOrNull("method", method.name(), String::class.java)
            .bindValueOrNull("rq_size", rqSize, Long::class.java)
            .bindValueOrNull("rq_time", rqTime, LocalDateTime::class.java)
            .bindValueOrNull("matched_route_details", mappingHelper.serialize(routeDetails), String::class.java)
            .bindValueOrNull("routed_service", routedService, String::class.java)
            .mapValue(BigInteger::class.java)
            .first()
            .map { it.longValueExact() }
            .subscribeOn(flexScheduler)
    }

    protected open val sqlSaveAuth =
        """
        update $schemaName.api_gateway_request_history set user_id=:user_id,account_id=:account_id,auth_details=:auth_details::jsonb
        where id=:id and rq_time=:rq_time
        returning id
        """.trimIndent()

    open fun saveAuth(
        idMono: Mono<Long>,
        rqTime: LocalDateTime,
        userId: UUID?,
        accountId: UUID?,
        authDetails: Any?,
    ): Mono<Long> {
        return idMono.flatMap { id ->
            return@flatMap dbClient.sql(sqlSaveAuth)
                .bindValueOrNull("id", id, Long::class.java)
                .bindValueOrNull("rq_time", rqTime, LocalDateTime::class.java)
                .bindValueOrNull("user_id", userId, UUID::class.java)
                .bindValueOrNull("account_id", accountId, UUID::class.java)
                .bindValueOrNull("auth_details", mappingHelper.serialize(authDetails), String::class.java)
                .mapValue(BigInteger::class.java)
                .first()
                .map { it.longValueExact() }
                .subscribeOn(flexScheduler)
        }
    }

    protected open val sqlSaveRs =
        """
        update $schemaName.api_gateway_request_history set rs_time=:rs_time,rs_size=:rs_size,rs_code=:rs_code
        where id=:id and rq_time=:rq_time
        returning id
        """.trimIndent()

    open fun saveRs(
        idMono: Mono<Long>,
        rqTime: LocalDateTime,
        rsTime: LocalDateTime,
        rsSize: Long?,
        rsCode: Int?,
    ): Mono<Long> {
        return idMono.flatMap { id ->
            return@flatMap dbClient.sql(sqlSaveRs)
                .bindValueOrNull("id", id, Long::class.java)
                .bindValueOrNull("rq_time", rqTime, LocalDateTime::class.java)
                .bindValueOrNull("rs_time", rsTime, LocalDateTime::class.java)
                .bindValueOrNull("rs_size", rsSize, Long::class.java)
                .bindValueOrNull("rs_code", rsCode, Int::class.java)
                .mapValue(BigInteger::class.java)
                .first()
                .map { it.longValueExact() }
                .subscribeOn(flexScheduler)
        }
    }

    fun DatabaseClient.GenericExecuteSpec.bindValueOrNull(
        name: String,
        value: Any?,
        valueClass: Class<*>,
    ): DatabaseClient.GenericExecuteSpec {
        val executeSpec =
            if (value == null) {
                bindNull(name, valueClass)
            } else {
                bind(name, value)
            }
        return executeSpec
    }
}
