package com.reborn.server.domain.place

import org.springframework.data.jpa.repository.JpaRepository

interface UserPlaceMappingRepository : JpaRepository<UserPlaceMapping, Long> {

    fun findByUserIdAndPlaceId(userId: Long, placeId: Long): UserPlaceMapping?

    fun findAllByUserId(userId: Long): List<UserPlaceMapping>

    fun findAllByPlaceId(placeId: Long): List<UserPlaceMapping>

    fun existsByUserIdAndPlaceId(userId: Long, placeId: Long): Boolean
}
