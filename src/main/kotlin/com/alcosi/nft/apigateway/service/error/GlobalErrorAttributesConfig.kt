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

package com.alcosi.nft.apigateway.service.error

import org.apache.logging.log4j.kotlin.Logging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * GlobalErrorAttributesConfig is a class that represents the configuration
 * for global error attributes in a Spring Boot application.
 *
 * It is annotated with @Configuration to indicate that it is a configuration
 * class. It also extends the Logging interface to enable logging capabilities.
 *
 * @see Configuration
 * @see Logging
 */
@Configuration
class GlobalErrorAttributesConfig : Logging {
    /**
     * Retrieves the global error attributes for the Spring Boot application.
     *
     * The method returns a DefaultErrorAttributes instance that provides custom error attribute handling.
     * It overrides the getErrorAttributes function to customize the attributes returned in case of an error.
     *
     * @return A DefaultErrorAttributes instance that handles global error attributes.
     * @see DefaultErrorAttributes
     */
    @Bean
    @ConditionalOnMissingBean(value = [GlobalErrorAttributes::class])
    fun getGlobalErrorAttributes(): DefaultErrorAttributes {
        return GlobalErrorAttributes()
    }
}
