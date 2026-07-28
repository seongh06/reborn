package com.reborn.server.domain.googlesheets

import org.springframework.data.jpa.repository.JpaRepository

interface GoogleSheetsCredentialRepository : JpaRepository<GoogleSheetsCredential, Long> {

    fun findByPlaceId(placeId: Long): GoogleSheetsCredential?

    fun existsByPlaceId(placeId: Long): Boolean
}
