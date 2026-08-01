package com.reborn.server.domain.device

import com.reborn.server.domain.place.Place
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

@Entity
@Table(name = "device")
class Device(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    val place: Place,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val deviceType: DeviceType,

    @Column(nullable = false, unique = true)
    val deviceKey: String,

    @Column
    val name: String? = null,

    // 기기 목록 아이콘 구분용(LAMP/PLUG/TV/AIR_CONDITIONER/AIR_PURIFIER/VENTILATOR/OTHER, core:ui DeviceType과 이름을
    // 맞춤) - SmartThings API가 기기 카테고리를 안정적으로 안 내려줘서 등록 시 관리자가 직접 선택.
    // ARDUINO/AEROMETER/AI_SPEAKER는 애초에 가전이 아니라 선택 UI 자체가 없어 항상 null(OTHER로 표시).
    @Column
    var category: String? = null,

    @Column
    var appToken: String? = null,

    @Column(nullable = false)
    var isOnline: Boolean = false,

    // ARDUINO 기기에 IR 송신 모듈이 실제로 물려있는지(#288) - 관리자가 등록 시 직접 설정. 물리적으로
    // 확인할 방법이 서버엔 없어서 신뢰 기반 플래그. SmartThings/AEROMETER/AI_SPEAKER는 항상 false.
    @Column(nullable = false)
    var hasIrControl: Boolean = false,

    // 관리자가 보낸 IR 명령 중 아두이노가 아직 폴링해가지 않은 것(#288) - 폴링 시 1회성으로 소비되고
    // 즉시 null로 비워짐(큐 길이 1, 이전 명령 대기 중 새 명령이 오면 그냥 덮어씀 - MVP 스코프).
    @Column
    @Enumerated(EnumType.STRING)
    var pendingIrCommand: IrCommand? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun updateOnlineStatus(online: Boolean) {
        isOnline = online
    }

    fun updateAppToken(token: String?) {
        appToken = token
    }

    fun queuePendingIrCommand(command: IrCommand) {
        pendingIrCommand = command
    }

    fun clearPendingIrCommand() {
        pendingIrCommand = null
    }
}
