package com.reborn.server.domain.analytics

import com.reborn.server.domain.device.Device
import com.reborn.server.global.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// 자동 제어가 실제로 얼마나/어떻게 실행되는지 확인하기 위한 이력(#222) - 실제 전력계 연동이 없어
// kWh 등 에너지 절감량 환산은 하지 않고 실행 횟수/내역만 쌓아둔다. device는 metric_logs/feedback과
// 동일하게 기기 삭제 시 SET NULL로 이력을 보존한다.
@Entity
@Table(name = "auto_control_execution_log")
class AutoControlExecutionLog(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    val device: Device?,

    @Column(nullable = false)
    val triggeredTemperature: Double,

    @Column(nullable = false)
    val action: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity()
