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
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties

/**
 * The DataSourceR2DBCConfigBeanPostProcessor class is a BeanPostProcessor implementation
 * that converts the R2DBC URL of a DataSourceProperties bean into a JDBC URL using
 * the provided R2DBCtoJDBCUriConverter.
 *
 * @param r2DBCtoJDBCUriConverter The R2DBCtoJDBCUriConverter used to convert the R2DBC URL to a JDBC URL.
 */
open class DataSourceR2DBCConfigBeanPostProcessor(val r2DBCtoJDBCUriConverter: R2DBCtoJDBCUriConverter) : BeanPostProcessor {
    /**
     * Performs pre-processing tasks before initializing the bean.
     *
     * @param bean The bean instance being initialized.
     * @param beanName The name of the bean.
     * @return The initialized bean instance.
     */
    override fun postProcessBeforeInitialization(
        bean: Any,
        beanName: String,
    ): Any? {
        if (bean is DataSourceProperties) {
            val r2dbc = bean.url
            val jdbc = r2DBCtoJDBCUriConverter.uri(r2dbc)
            bean.url = jdbc
        }
        return super.postProcessBeforeInitialization(bean, beanName)
    }
}
