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

import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer

/**
 * R2DBCConnectionFactoryOptionsBuilderCustomizer is a class that implements the ConnectionFactoryOptionsBuilderCustomizer interface.
 * It customizes the ConnectionFactoryOptions.Builder by adding a specific option to the options map.
 *
 * @property options The map of options to be added to the ConnectionFactoryOptions.Builder.
 */
open class R2DBCConnectionFactoryOptionsBuilderCustomizer(val options: Map<String, String>) : ConnectionFactoryOptionsBuilderCustomizer {
    /**
     * Protected open property representing an option for the R2DBC connection factory.
     * This option is used to configure a map of key-value pairs to be added to the ConnectionFactoryOptions.Builder.
     * The option is of type `Option<Map<String, String>>`.
     *
     * @see R2DBCConnectionFactoryOptionsBuilderCustomizer
     * @see ConnectionFactoryOptions.Builder
     * @see ConnectionFactoryOptions.Builder.option
     */
    protected open val option = Option.valueOf<Map<String, String>>("options")

    /**
     * Customize the ConnectionFactoryOptions.Builder by adding a specific option to the options map.
     *
     * @param builder The ConnectionFactoryOptions.Builder to be customized.
     *
     * @see ConnectionFactoryOptions.Builder
     * @see ConnectionFactoryOptions.Builder.option
     */
    override fun customize(builder: ConnectionFactoryOptions.Builder) {
        builder.option(option, options)
    }
}
