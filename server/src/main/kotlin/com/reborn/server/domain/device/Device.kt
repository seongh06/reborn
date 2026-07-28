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

    // 기기 목록 아이콘 구분용(LAMP/PLUG/TV/AIR_CONDITIONER/CURTAIN/OTHER, core:ui DeviceType과 이름을
    // 맞춤) - SmartThings API가 기기 카테고리를 안정적으로 안 내려줘서 등록 시 관리자가 직접 선택.
    // ARDUINO/AEROMETER/AI_SPEAKER는 애초에 가전이 아니라 선택 UI 자체가 없어 항상 null(OTHER로 표시).
    @Column
    var category: String? = null,

    @Column
    var appToken: String? = null,

    @Column(nullable = false)
    var isOnline: Boolean = false,

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
}
