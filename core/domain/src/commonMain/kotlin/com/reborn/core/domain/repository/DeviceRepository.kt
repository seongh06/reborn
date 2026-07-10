package com.reborn.core.domain.repository

import com.reborn.core.model.PairedDevice
import com.reborn.core.model.PairingCode

interface DeviceRepository {
    suspend fun generatePairingCode(placeId: Long): Result<PairingCode>

    suspend fun pairDevice(pairingCode: String, deviceName: String): Result<PairedDevice>
}
