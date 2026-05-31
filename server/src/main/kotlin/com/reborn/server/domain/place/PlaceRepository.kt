package com.reborn.server.domain.place

import org.springframework.data.jpa.repository.JpaRepository

interface PlaceRepository : JpaRepository<Place, Long> {

    fun findByQrCode(qrCode: String): Place?

    fun existsByQrCode(qrCode: String): Boolean
}
