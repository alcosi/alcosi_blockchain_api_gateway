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

/**
 * The `ApiGatewayApplication` class is the entry point for the API Gateway application.
 * It is a Spring Boot application that enables scheduling and configures various settings.
 *
 * The `@EnableScheduling` annotation enables scheduling support for the application.
 *
 * The `@SpringBootApplication` annotation is used to enable the Spring Boot features and
 * configure the application. It takes the following parameters:
 * - `scanBasePackages`: Specifies the base packages to scan for components, such as controllers,
 *   services, and repositories.
 * - `exclude`: Excludes the specified auto-configuration classes from the application context.
 *   In this case, Redis, R2DBC, JDBC, and Flyway-related auto-configurations are excluded.
 *
 */
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

/**
 * The main entry point for the application.
 *
 * This method initializes and runs the API Gateway application. It loads external jars,
 * sets the web application type to reactive, and starts the application.
 *
 * @param args The command line arguments.
 * @see ExternalJarLoad.loadDependency
 * @see SpringApplicationBuilder
 * @see ApiGatewayApplication
 */
fun main(args: Array<String>) {

// Here we are checking and setting "reactor.schedulers.defaultBoundedElasticOnVirtualThreads" system property based on the environment variable value.
// If the corresponding environment variable is not set to "false", we are setting the system property to "true".
// This property is used by Reactor to determine if the scheduler should use virtual threads.
    val boundedElasticVirtual=System.getenv()["reactor.schedulers.defaultBoundedElasticOnVirtualThreads"]
    if (!"false".contentEquals(boundedElasticVirtual)) {
        System.setProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads", "true")
    }
    println("Test msg")
    val externalJarDir = System.getenv()["external.jar.directory.path"] ?: "/opt/external-jar"
    ExternalJarLoad().loadDependency(listOf(Path(externalJarDir)), true)
    SpringApplicationBuilder(ApiGatewayApplication::class.java)
        .web(WebApplicationType.REACTIVE)
        .run("")
}
