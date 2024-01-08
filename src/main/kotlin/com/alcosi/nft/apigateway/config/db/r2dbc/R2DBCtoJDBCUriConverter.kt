package com.alcosi.nft.apigateway.config.db.r2dbc

open class R2DBCtoJDBCUriConverter {
    open fun uri(value: String?): String? {
        val pool = value?.replace(":pool", "");
        val jdbc = pool?.replace("r2dbc", "jdbc")
        return jdbc
    }
}