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

package com.alcosi.nft.apigateway.config.path

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the Path Configuration component.
 *
 * This class provides configuration properties for the Path Configuration component. It is annotated with
 * `@ConfigurationProperties` to define a prefix for the properties. The prefix is "filter.config.path", which means
 * that the properties can be configured using the key-value format "filter.config.path.propertyName".
 *
 * This class has three properties:
 * - `validation`: A map of validation configurations. The keys represent the route names and the values are the
 *   configuration values. It is initialized with an empty map.
 * - `security`: A map of security configurations. The keys represent the route names and the values are the configuration
 *   values. It is initialized with an empty map.
 * - `proxy`: A map of proxy configurations. The keys represent the route names and the values are the configuration
 *   values. It is initialized with an empty map.
 *
 */
@ConfigurationProperties(prefix = "filter.config.path")
class PathConfigurationProperties {
    /**
     * Represents a map of validation configurations.
     *
     * The keys in the map represent the route names, and the values are the configuration values.
     * This map is used to store the validation route configurations.
     *
     * @property validation The map of validation configurations.
     */
    var validation: MutableMap<String, Any?> = mutableMapOf()

    /**
     * Represents a map of security configurations.
     *
     * The keys in the map represent the route names, and the values are the configuration values.
     * This map is used to store the security route configurations.
     *
     * @property security The map of security configurations.
     */
    var security: MutableMap<String, Any?> = mutableMapOf()

    /**
     * Represents a mutable map of proxy configurations.
     *
     * This variable is used to store proxy route configurations. The keys in the map represent the route names, and the values are the configuration values. The configuration values
     *  are JSON strings representing the route configurations.
     *
     * @property proxy The mutable map of proxy configurations. The keys represent the route names, and the values are nullable strings.
     */
    var proxy: MutableMap<String, String?> = mutableMapOf()
}
