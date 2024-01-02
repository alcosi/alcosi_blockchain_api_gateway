package com.alcosi.nft.apigateway.service.gateway.filter.security.oath2

import com.alcosi.lib.security.AccountDetails

open class ClientAccountDetails(
    id: String,
    authorities: List<String>,
    val clientId: String?,
    ) : AccountDetails(id, authorities) {

}