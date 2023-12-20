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
