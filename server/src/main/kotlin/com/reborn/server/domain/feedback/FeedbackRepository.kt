package com.reborn.server.domain.feedback

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepository : JpaRepository<Feedback, Long> {

    fun findAllByDeviceId(deviceId: Long, pageable: Pageable): Page<Feedback>

    fun findAllByDeviceIdAndStatus(deviceId: Long, status: FeedbackStatus, pageable: Pageable): Page<Feedback>

    fun countByDeviceId(deviceId: Long): Long

    // device가 없는 피드백도 이 장소 목록/개수에 포함돼야 하므로, device를 거치는 프로퍼티 경로
    // (device_PlaceId - 암묵적 INNER JOIN이라 device=null 행이 통째로 빠짐) 대신 place_id를
    // 직접 조회한다.
    fun findAllByPlaceId(placeId: Long, pageable: Pageable): Page<Feedback>

    fun findAllByPlaceIdAndStatus(placeId: Long, status: FeedbackStatus, pageable: Pageable): Page<Feedback>

    fun countByPlaceId(placeId: Long): Long

    fun countByPlaceIdAndStatus(placeId: Long, status: FeedbackStatus): Long

    // 기기 삭제(ON DELETE SET NULL) 후 device_id = NULL 상태인 고아 피드백 조회
    fun findAllByDeviceIsNull(pageable: Pageable): Page<Feedback>

    fun countByDeviceIsNull(): Long

    fun existsBySessionToken(sessionToken: String): Boolean
}
