package com.alcosi.nft.apigateway.service.validation

import java.math.BigDecimal

@JvmRecord
data class ValidationResult(
    val success: Boolean,
    val score: BigDecimal,
    val errorDescription: String? = null
)
    