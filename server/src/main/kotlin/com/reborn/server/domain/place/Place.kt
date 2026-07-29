package com.reborn.server.domain.place

import com.reborn.server.global.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "place")
class Place(

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false, unique = true)
    val qrCode: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PlaceType,

    @Column
    val description: String? = null,

    // 아두이노/AI스피커가 SoftAP 프로비저닝으로 붙을 홈 WiFi(#219) - 장소에 한 번 저장해두면
    // 이후 이 장소에 연결되는 모든 기기가 관리자의 재입력 없이 이 값을 그대로 사용한다.
    @Column
    var wifiSsid: String? = null,

    @Column
    var wifiPassword: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun updateWifi(ssid: String, password: String) {
        wifiSsid = ssid
        wifiPassword = password
    }
}
