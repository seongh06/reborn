package com.reborn.core.network.model.response.googlesheets

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSheetsAuthorizeResponse(
    val authorizeUrl: String,
)
