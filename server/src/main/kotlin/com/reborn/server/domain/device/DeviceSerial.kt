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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

// 판매용 기기 사전 발급 시리얼 재고(#147). 서비스 운영자가 배치로 미리 생성해두고, 실물 등록 시점에
// deviceKey로 소비된다 — device 테이블(place_id NOT NULL)과 달리 place 매핑 전 상태를 표현해야 해서
// 별도 테이블로 둔다.
@Entity
@Table(name = "device_serial")
class DeviceSerial(

    @Column(nullable = false, unique = true, length = 8)
    val serial: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val deviceType: DeviceType,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_device_id", unique = true)
    var assignedDevice: Device? = null,

    @Column
    var assignedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun assignTo(device: Device) {
        assignedDevice = device
        assignedAt = LocalDateTime.now()
    }
}
