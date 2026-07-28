package com.reborn.server.domain.device

import com.reborn.server.global.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

// 기기당 자동 제어 규칙 1건 (#190). 클라이언트가 자유 텍스트로 편집하는 값 그대로 저장하고(예:
// "28°C", "70%", "5명"), 실제 조건 평가 시점(AutoControlEvaluationService)에만 숫자를 파싱한다 -
// 클라이언트 UI를 숫자 입력으로 바꾸지 않고도 저장은 실 데이터로 만들기 위한 절충.
@Entity
@Table(name = "auto_control_rule")
class AutoControlRule(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    val device: Device,

    @Column
    var discomfortThreshold: String? = null,
    @Column
    var discomfortAction: String? = null,

    @Column
    var humidityHighThreshold: String? = null,
    @Column
    var humidityHighAction: String? = null,
    @Column
    var humidityLowThreshold: String? = null,
    @Column
    var humidityLowAction: String? = null,

    @Column
    var temperatureHighThreshold: String? = null,
    @Column
    var temperatureHighAction: String? = null,
    @Column
    var temperatureLowThreshold: String? = null,
    @Column
    var temperatureLowAction: String? = null,

    @Column
    var occupancyThreshold: String? = null,
    @Column
    var occupancyAction: String? = null,

    @Column(nullable = false)
    var isAutoOffEnabled: Boolean = false,
    @Column
    var autoOffMinutes: String? = null,

    // 스케줄러가 동일 조건에 매 tick마다 반복 실행하지 않도록 하는 쿨다운 기준 시각 - 온도 조건만
    // 우선 실행하므로(아래 참고) 이 필드도 온도 조건 트리거에만 쓰인다.
    @Column
    var lastTriggeredAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity()
