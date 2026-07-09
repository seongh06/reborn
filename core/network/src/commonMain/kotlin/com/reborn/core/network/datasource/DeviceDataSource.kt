package com.reborn.core.network.datasource

import com.reborn.core.network.model.ApiResponse
import com.reborn.core.network.model.response.device.PairingCodeResponse

interface DeviceDataSource {
    suspend fun generatePairingCode(accessToken: String, placeId: Long): ApiResponse<PairingCodeResponse>
}
