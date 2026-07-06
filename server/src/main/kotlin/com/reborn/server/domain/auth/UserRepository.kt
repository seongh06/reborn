package com.reborn.server.domain.auth

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): User?

    fun existsByEmail(email: String): Boolean
}
