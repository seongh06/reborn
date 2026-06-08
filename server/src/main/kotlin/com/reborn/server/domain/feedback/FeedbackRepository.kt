package com.reborn.server.domain.feedback

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepository : JpaRepository<Feedback, Long> {

    fun findAllByDeviceId(deviceId: Long, pageable: Pageable): Page<Feedback>

    fun findAllByDeviceIdAndStatus(deviceId: Long, status: FeedbackStatus, pageable: Pageable): Page<Feedback>

    fun countByDeviceId(deviceId: Long): Long

    // 기기 삭제(ON DELETE SET NULL) 후 device_id = NULL 상태인 고아 피드백 조회
    fun findAllByDeviceIsNull(pageable: Pageable): Page<Feedback>

    fun countByDeviceIsNull(): Long

    fun existsBySessionToken(sessionToken: String): Boolean
}
