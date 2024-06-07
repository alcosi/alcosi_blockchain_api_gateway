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

import com.alcosi.lib.objectMapper.serialize
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpMethod
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

/**
 * Represents a component for managing the history of requests in a database.
 *
 * @property flexScheduler The scheduler used for executing the database operations asynchronously.
 * @property dbClient The client for interacting with the database.
 * @property mappingHelper The helper class for mapping objects to JSON strings.
 * @property schemaName The name of the database schema for storing request history. Defaults to "request_history".
 */
open class RequestHistoryDBComponent(
    protected val flexScheduler: Scheduler,
    protected val dbClient: DatabaseClient,
    protected val mappingHelper: ObjectMapper,
    schemaName: String = "request_history",
) {
    /**
     * Represents a SQL save request for the API Gateway request history.
     * The request is used to insert a new request record into the specified table.
     * The request includes various fields such as the request ID, headers, IP address, URI, method, request size, request time,
     * routed service, and matched route details.
     *
     * The request is constructed using a parameterized SQL query, where the actual values are provided during execution.
     * The query inserts the request data into the table defined by the 'schemaName' property, and returns the ID of the inserted record.
     *
     * @see [schemaName]
     */
    protected open val sqlSaveRequest = """insert into $schemaName.api_gateway_request_history(rq_id,rq_headers, ip, uri, method,rq_size,  rq_time,routed_service,  matched_route_details)
                 values (:rq_id,:rq_headers::jsonb,:ip,:uri,:method::$schemaName.http_method_type,:rq_size,:rq_time,:routed_service,:matched_route_details::jsonb)
                 returning id"""

    /**
     * Saves a request to the database.
     *
     * @param rqId The ID of the request.
     * @param rqHeaders The headers of the request.
     * @param ip The IP address of the client.
     * @param uri The URI of the request.
     * @param method The HTTP method of the request.
     * @param rqSize The size of the request in bytes.
     * @param rqTime The time at which the request was made.
     * @param routedService The service through which the request was routed.
     * @param routeDetails Additional details about the route.
     * @return A Mono emitting the ID of the saved request as a Long.
     */
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

    /**
     * SQL query for updating authentication details in the database.
     *
     * This query updates the "api_gateway_request_history" table in the schema specified by "schemaName".
     * It sets the "user_id", "account_id", and "auth_details" columns to the specified values.
     * The query uses parameterized values for "user_id", "account_id", "id", and "rq_time".
     * The authentication details are expected to be in JSON format and are cast to the JSONB datatype in the database.
     *
     * @see schemaName
     */
    protected open val sqlSaveAuth =
        """
        update $schemaName.api_gateway_request_history set user_id=:user_id,account_id=:account_id,auth_details=:auth_details::jsonb
        where id=:id and rq_time=:rq_time
        returning id
        """.trimIndent()

    /**
     * Saves authentication information to the database.
     *
     * @param idMono A Mono that represents the identifier of the authentication.
     * @param rqTime The LocalDateTime of the authentication request.
     * @param userId The UUID of the user associated with the authentication.
     * @param accountId The UUID of the account associated with the authentication.
     * @param authDetails Any additional authentication details.
     * @return A Mono that emits the ID of the saved authentication.
     */
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

    /**
     * SQL query string used to update the "api_gateway_request_history" table and return the ID.
     *
     * The query updates the "rs_time", "rs_size", and "rs_code" columns in the "api_gateway_request_history" table
     * based on the provided parameters. The query filters the records based on the "id" and "rq_time" columns.
     * After updating the records, the query returns the "id" column value of the updated record.
     *
     * @see schemaName.api_gateway_request_history
     *
     * @returns SQL query string
     */
    protected open val sqlSaveRs =
        """
        update $schemaName.api_gateway_request_history set rs_time=:rs_time,rs_size=:rs_size,rs_code=:rs_code
        where id=:id and rq_time=:rq_time
        returning id
        """.trimIndent()

    /**
     * Save the response data to the database.
     *
     * @param idMono The ID of the request.
     * @param rqTime The time when the request was made.
     * @param rsTime The time when the response was received.
     * @param rsSize The size of the response data.
     * @param rsCode The response code.
     * @return A Mono emitting the ID of the saved response.
     */
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

    /**
     * Binds a value to a named parameter, or binds a null value if the provided value is null.
     *
     * @param name The name of the parameter to bind.
     * @param value The value to bind to the parameter. If null, a null value will be bound.
     * @param valueClass The class of the value being bound.
     * @return The modified DatabaseClient.GenericExecuteSpec instance with the parameter bound.
     */
    open fun DatabaseClient.GenericExecuteSpec.bindValueOrNull(
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
