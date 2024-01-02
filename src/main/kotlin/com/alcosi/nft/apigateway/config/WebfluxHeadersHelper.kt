package com.alcosi.nft.apigateway.config

import org.springframework.http.server.reactive.ServerHttpRequest

object WebfluxHeadersHelper {
     fun getHeaderOrQuery(request: ServerHttpRequest, headerName: String): String? {
        val tokenString = request.headers[headerName]?.firstOrNull()
        return if (tokenString.isNullOrEmpty()) {
            request.queryParams[headerName]?.firstOrNull()
        } else tokenString
    }
}