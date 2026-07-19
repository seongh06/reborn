package com.reborn.server.domain.smartthings

import org.springframework.data.jpa.repository.JpaRepository

interface SmartThingsCredentialRepository : JpaRepository<SmartThingsCredential, Long> {

    fun findByPlaceId(placeId: Long): SmartThingsCredential?

    fun existsByPlaceId(placeId: Long): Boolean
}
