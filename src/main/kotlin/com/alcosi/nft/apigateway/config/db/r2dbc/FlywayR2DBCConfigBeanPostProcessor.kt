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

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.flyway.FlywayProperties

open class FlywayR2DBCConfigBeanPostProcessor(val r2DBCtoJDBCUriConverter: R2DBCtoJDBCUriConverter) : BeanPostProcessor {
    override fun postProcessBeforeInitialization(
        bean: Any,
        beanName: String,
    ): Any? {
        if (bean is FlywayProperties) {
            val r2dbc = bean.url
            val jdbc = r2DBCtoJDBCUriConverter.uri(r2dbc)
            bean.url = jdbc
        }
        return super.postProcessBeforeInitialization(bean, beanName)
    }
}
