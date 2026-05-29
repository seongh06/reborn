package com.reborn.server.global.handler

import com.reborn.server.global.model.CommonErrorCode

class BusinessAlertException(
    val errorCode: CommonErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
