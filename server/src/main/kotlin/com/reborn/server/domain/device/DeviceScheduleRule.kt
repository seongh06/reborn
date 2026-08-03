package com.reborn.server.domain.device

import com.reborn.server.global.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalDateTime

// 시간 기반 자동제어 규칙(#325) - 센서 조건 기반인 AutoControlRule(#190)과 별개 축. 기기당 여러 건
// 등록 가능해서(예: "07시 켜짐" + "22시 꺼짐") device에 OneToOne이 아니라 ManyToOne을 건다.
@Entity
@Table(name = "device_schedule_rule")
class DeviceScheduleRule(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,

    @Column(nullable = false)
    var hour: Int,

    @Column(nullable = false)
    var minute: Int,

    // CSV로 저장(예: "MONDAY,WEDNESDAY,FRIDAY") - DayOfWeek 7개 조합이라 비트마스크도 가능하지만,
    // auto_control_rule의 자유 텍스트 저장 관례를 따라 사람이 읽을 수 있는 형태를 우선한다.
    @Column(name = "days_of_week", nullable = false, length = 60)
    var daysOfWeekCsv: String,

    @Column(name = "is_power_on", nullable = false)
    var isPowerOn: Boolean,

    @Column(name = "operation_mode")
    @Enumerated(EnumType.STRING)
    var operationMode: OperationMode? = null,

    @Column(nullable = false)
    var enabled: Boolean = true,

    // 같은 분에 스케줄러가 중복 실행(재시작 직후 겹침 등)하지 않도록 하는 가드 - AutoControlRule의
    // 쿨다운(#190, 30분)과 달리 여기는 "이번 정각 1분" 단위로만 막으면 충분해 별도 Duration 상수 없이
    // evaluate() 쪽에서 분 단위로 직접 비교한다.
    @Column(name = "last_triggered_at")
    var lastTriggeredAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    val daysOfWeek: Set<DayOfWeek>
        get() = daysOfWeekCsv.split(",").filter { it.isNotBlank() }.map { DayOfWeek.valueOf(it) }.toSet()

    fun matches(now: LocalDateTime): Boolean =
        enabled && now.hour == hour && now.minute == minute && now.dayOfWeek in daysOfWeek

    fun alreadyTriggeredThisMinute(now: LocalDateTime): Boolean {
        val last = lastTriggeredAt ?: return false
        return last.year == now.year && last.dayOfYear == now.dayOfYear &&
            last.hour == now.hour && last.minute == now.minute
    }

    companion object {
        fun encodeDaysOfWeek(days: Set<DayOfWeek>): String = days.joinToString(",") { it.name }
    }
}
