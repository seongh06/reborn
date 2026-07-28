package com.reborn.core.network.remote

import com.reborn.core.network.datasource.GoogleSheetsDataSource
import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.googlesheets.GoogleSheetsAuthorizeResponse
import com.reborn.core.network.util.asApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class GoogleSheetsDataSourceImpl(
    private val httpClient: HttpClient,
) : GoogleSheetsDataSource {

    override suspend fun getAuthorizeUrl(placeId: Long): ApiResponse<GoogleSheetsAuthorizeResponse> = runCatching {
        httpClient.get("/api/google-sheets/oauth/authorize") {
            parameter("placeId", placeId)
        }
    }.asApiResponse()
}
