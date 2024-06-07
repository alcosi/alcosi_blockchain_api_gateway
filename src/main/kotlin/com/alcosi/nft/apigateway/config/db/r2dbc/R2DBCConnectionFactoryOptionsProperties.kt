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

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for R2DBC connection factory options.
 *
 * @property enabled Whether R2DBC connection factory options are enabled. Default is false.
 * @property threads The number of threads to use for R2DBC connection factory options. Default is 10.
 * @property maxTasks The maximum number of tasks to allow in R2DBC connection factory options. Default is 10.
 * @property requestHistoryFilterOrder The order of the request history filter. Default is -2147483648.
 * @property requestHistoryIpHeader The IP header to use for request history. Default is "x-real-ip".
 * @property partitionsInitSchedulerDelay The delay for initializing partitions. Default is 1 day.
 * @property partitionsInitMonthDelta The month delta for initializing partitions. Default is 2.
 * @property requestHistoryMaskHeaders The list of headers to mask in request history. Default is ["AUTHORIZATION", "ValidationToken"].
 * @property options Additional options for R2DBC connection factory. Default is an empty map.
 */
@ConfigurationProperties(prefix = "spring.r2dbc")
open class R2DBCConnectionFactoryOptionsProperties {
    /**
     * Represents whether R2DBC connection factory options are enabled.
     * The default value is false.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     */
    var enabled: Boolean = false

    /**
     * Number of threads to use for R2DBC connection factory options.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     */
    var threads: Int = 10

    /**
     * Maximum number of tasks allowed in the R2DBC connection factory options.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     */
    var maxTasks: Int = 10

    /**
     * Represents the filter order of the request history filter.
     *
     * The request history filter is responsible for saving the request history information, including the request headers
     * and the response details. This filter will be executed based on the filter order value.
     * A lower value indicates a higher priority, i.e., filters with a lower value will be executed before filters with a higher value.
     *
     * The default value for this variable is -2147483648, which is the minimum value for an `Int`
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     */
    var requestHistoryFilterOrder: Int = -2147483648

    /**
     * The IP header used to retrieve the client IP address from incoming requests.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     *
     */
    var requestHistoryIpHeader: String = "x-real-ip"

    /**
     * Represents the delay for initializing partitions in the scheduler.
     * The value is a Duration object representing a time span of 1 day.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     *
     */
    var partitionsInitSchedulerDelay: Duration = Duration.ofDays(1)

    /**
     * The number of months to initialize partitions for.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     *
     */
    var partitionsInitMonthDelta: Int = 2

    /**
     * Holds a list of headers that should be masked in the request history.
     * This variable is used in the `RequestHistoryDBService` class for saving requests and responses.
     * The headers specified in this list will be replaced with "<masked>" when saved in the request history.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     *
     */
    var requestHistoryMaskHeaders: List<String> = listOf("AUTHORIZATION", "ValidationToken")

    /**
     * options is a variable of type Map<String, String>.
     *
     * It represents a map of options that can be used for customization or configuration purposes.
     *
     * The keys in the map are of type String, representing the names of the options.
     *
     * The values in the map are of type String, representing the values associated with the options.
     *
     * @see R2DBCConnectionFactoryOptionsProperties
     *
     */
    var options: MutableMap<String, String> = mutableMapOf()
}
