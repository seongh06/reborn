package com.reborn.server.domain.analytics

import com.reborn.server.global.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// 릴리즈 환경에서 API가 언제/얼마나 호출되는지 확인하기 위한 요청 로그 영속화(#222) - 기존
// LoggingInterceptor는 stdout/Docker 로그로만 남겨 휘발성이고 집계가 불가능했음. userId는 FK가
// 아니라 단순 참조 컬럼 - 탈퇴한 사용자의 과거 요청 이력도 그대로 보존하고, 고빈도 쓰기 테이블에
// FK 오버헤드를 두지 않기 위함(user_id NULL이면 비인증 요청).
@Entity
@Table(name = "api_request_log")
class ApiRequestLog(

    @Column(nullable = false, length = 10)
    val method: String,

    @Column(nullable = false)
    val path: String,

    @Column(nullable = false)
    val status: Int,

    @Column
    val userId: Long? = null,

    @Column(nullable = false)
    val elapsedMs: Long,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity()
