package com.reborn.core.data.mapper

import com.reborn.core.model.PairingCode
import com.reborn.core.network.model.response.device.PairingCodeResponse

fun PairingCodeResponse.toPairingCode(): PairingCode =
    PairingCode(code = pairingCode, expiresAt = expiresAt)
