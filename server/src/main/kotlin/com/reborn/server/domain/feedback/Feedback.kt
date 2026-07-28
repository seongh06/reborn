package com.reborn.server.domain.feedback

import com.reborn.server.domain.device.Device
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
@Table(name = "feedback")
class Feedback(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    val device: Device?,

    @Column(nullable = false, length = 1000)
    val content: String,

    @Column
    val sessionToken: String? = null,

    @Column(length = 1024)
    val userAgent: String? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val source: FeedbackSource = FeedbackSource.QR,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    // AI 맞춤 피드백(제출 시점 센서 스냅샷 + 추천 희망 온도) - 제출 직후 비동기로 채워지므로
    // 전부 nullable. Gemini 미설정/실패/최신 메트릭 없음 등으로 영영 null로 남을 수 있음.
    @Column
    var snapshotTemperature: Double? = null,

    @Column
    var snapshotHumidity: Double? = null,

    @Column
    var snapshotIlluminance: Int? = null,

    @Column
    var snapshotPeopleCount: Int? = null,

    @Column
    var recommendedTemperatureBefore: Double? = null,

    @Column
    var recommendedTemperatureAfter: Double? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun updateStatus(newStatus: FeedbackStatus) {
        status = newStatus
    }

    fun applyAiRecommendation(
        snapshotTemperature: Double?,
        snapshotHumidity: Double?,
        snapshotIlluminance: Int?,
        snapshotPeopleCount: Int?,
        recommendedTemperatureBefore: Double,
        recommendedTemperatureAfter: Double,
    ) {
        this.snapshotTemperature = snapshotTemperature
        this.snapshotHumidity = snapshotHumidity
        this.snapshotIlluminance = snapshotIlluminance
        this.snapshotPeopleCount = snapshotPeopleCount
        this.recommendedTemperatureBefore = recommendedTemperatureBefore
        this.recommendedTemperatureAfter = recommendedTemperatureAfter
    }
}
