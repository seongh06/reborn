package com.reborn.server.domain.feedback

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepository : JpaRepository<Feedback, Long> {

    fun findAllByDeviceId(deviceId: Long, pageable: Pageable): Page<Feedback>

    fun findAllByDeviceIdAndStatus(deviceId: Long, status: FeedbackStatus, pageable: Pageable): Page<Feedback>

    fun existsBySessionToken(sessionToken: String): Boolean
}
