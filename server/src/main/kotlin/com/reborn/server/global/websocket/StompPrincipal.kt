package com.reborn.server.global.websocket

import java.security.Principal

enum class ConnectionRole {
    ADMIN,
    KIOSK,
}

data class StompPrincipal(
    private val name: String,
    val role: ConnectionRole,
) : Principal {
    override fun getName(): String = name
}
