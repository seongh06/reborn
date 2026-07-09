package com.reborn.core.domain.repository

import com.reborn.core.model.PairingCode

interface DeviceRepository {
    suspend fun generatePairingCode(placeId: Long): Result<PairingCode>
}
