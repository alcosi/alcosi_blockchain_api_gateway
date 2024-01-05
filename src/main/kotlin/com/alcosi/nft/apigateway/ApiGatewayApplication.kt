/*
 * Copyright (c) 2023  Alcosi Group Ltd. and affiliates.
 *
 * Portions of this software are licensed as follows:
 *
 *     All content that resides under the "alcosi" and "atomicon" or “deploy” directories of this repository, if that directory exists, is licensed under the license defined in "LICENSE.TXT".
 *
 *     All third-party components incorporated into this software are licensed under the original license provided by the owner of the applicable component.
 *
 *     Content outside of the above-mentioned directories or restrictions above is available under the MIT license as defined below.
 *
 *
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is urnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.alcosi.nft.apigateway

import com.alcosi.lib.db.JdbcTemplateConfig
import com.alcosi.lib.object_mapper.ObjectMapperConfig
import com.alcosi.lib.utils.ExternalJarLoad
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling
import kotlin.io.path.Path

@EnableScheduling
@SpringBootApplication(
    scanBasePackages = [
        "com.alcosi.nft.apigateway",
        "\${scan.basePackage:com.alcosi.nft.apigateway}"
    ],exclude = [
        RedisAutoConfiguration::class,RedisReactiveAutoConfiguration::class,//Redis
        R2dbcAutoConfiguration::class, TransactionAutoConfiguration::class, R2dbcTransactionManagerAutoConfiguration::class,//R2DBC
        JdbcTemplateConfig::class //JDBC - never used
    ],
)
class ApiGatewayApplication


fun main(args: Array<String>) {
    val externalJarDir = System.getenv()["external.jar.directory.path"] ?: "/opt/external-jar"
    ExternalJarLoad().loadDependency(listOf(Path(externalJarDir)),true)
    SpringApplicationBuilder(ApiGatewayApplication::class.java)
        .web(WebApplicationType.REACTIVE)
        .run("")
}
