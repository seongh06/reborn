package com.reborn.server.domain.auth.converter

import com.reborn.server.domain.auth.User
import com.reborn.server.domain.auth.dto.AuthDto

object AuthConverter {

    fun toLoginResponse(user: User, accessToken: String, refreshToken: String, isNewUser: Boolean): AuthDto.LoginResponse =
        AuthDto.LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            name = user.name,
            isNewUser = isNewUser,
        )
}
