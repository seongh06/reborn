package com.reborn.core.data.repository

import com.reborn.core.data.datasource.AuthLocalDataSource
import com.reborn.core.data.mapper.toPairingCode
import com.reborn.core.data.mapper.toResult
import com.reborn.core.domain.repository.DeviceRepository
import com.reborn.core.model.PairingCode
import com.reborn.core.network.datasource.DeviceDataSource

class DeviceRepositoryImpl(
    private val remote: DeviceDataSource,
    private val local: AuthLocalDataSource,
) : DeviceRepository {

    override suspend fun generatePairingCode(placeId: Long): Result<PairingCode> =
        remote.generatePairingCode(local.getAccessToken().orEmpty(), placeId)
            .toResult { it.toPairingCode() }
}
