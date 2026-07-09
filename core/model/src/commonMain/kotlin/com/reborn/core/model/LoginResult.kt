package com.reborn.core.model

data class LoginResult(
    val userId: Long,
    val name: String,
    val isNewUser: Boolean,
)
