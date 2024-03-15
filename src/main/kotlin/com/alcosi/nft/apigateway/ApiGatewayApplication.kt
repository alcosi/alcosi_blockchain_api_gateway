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

package com.alcosi.nft.apigateway

import com.alcosi.lib.db.JdbcTemplateConfig
import com.alcosi.lib.utils.ExternalJarLoad
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.scheduling.annotation.EnableScheduling
import kotlin.io.path.Path

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "com.alcosi.nft.apigateway",
        "\${scan.basePackage:com.alcosi.nft.apigateway}",
    ],
    exclude = [
        RedisAutoConfiguration::class, RedisReactiveAutoConfiguration::class, // Redis
        R2dbcAutoConfiguration::class, TransactionAutoConfiguration::class, R2dbcTransactionManagerAutoConfiguration::class, // R2DBC
        DataSourceAutoConfiguration::class,
        FlywayAutoConfiguration::class,
        JdbcTemplateConfig::class, // JDBC - never used
    ],
)
class ApiGatewayApplication

fun main(args: Array<String>) {
    val externalJarDir = System.getenv()["external.jar.directory.path"] ?: "/opt/external-jar"
    ExternalJarLoad().loadDependency(listOf(Path(externalJarDir)), true)
    SpringApplicationBuilder(ApiGatewayApplication::class.java)
        .web(WebApplicationType.REACTIVE)
        .run("")
}
