package com.alcosi.nft.apigateway.config.db.r2dbc

import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer

open class R2DBCConnectionFactoryOptionsBuilderCustomizer(val options: Map<String,String>): ConnectionFactoryOptionsBuilderCustomizer {
    protected open val option = Option.valueOf<Map<String, String>>("options")

    override fun customize(builder: ConnectionFactoryOptions.Builder) {
        builder.option(option,options)
    }
}