package com.reborn.server.domain.auth

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?

    fun findByEmailAndProvider(email: String, provider: OAuthProvider): User?

    fun existsByEmail(email: String): Boolean
}
