package com.alcosi.nft.apigateway.config.db.r2dbc

interface FlywayMigrateCallback {
    fun call()
}