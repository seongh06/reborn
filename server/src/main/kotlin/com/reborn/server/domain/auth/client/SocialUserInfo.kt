package com.reborn.server.domain.auth.client

data class SocialUserInfo(
    val providerId: String,
    val email: String?,
    val name: String,
    val profileImage: String?,
)
