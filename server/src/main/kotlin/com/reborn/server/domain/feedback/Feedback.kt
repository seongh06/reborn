package com.reborn.server.domain.feedback

import com.reborn.server.domain.device.Device
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
@Table(name = "feedback")
class Feedback(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    val device: Device?,

    // device가 없어도(장소에 기기가 없거나 사용자가 지목하지 않음) 이 피드백이 어느 장소
    // 소속인지는 항상 알아야 목록/개수 조회·처리 권한 확인이 가능하다 - device.place로
    // 유추하면 device가 null일 때 조회 쿼리(JOIN)에서 통째로 누락되므로 별도 FK로 둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    val place: Place,

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

    // 승인/거절(status)과는 직교하는 별도 축(#318) - 관리자가 상세를 열어본 적 있는지. 온도 조절이
    // 아닌(승인/거절할 IoT 액션이 없는) 피드백은 읽고 나면 이 값만 true가 되고 status는 영원히
    // PENDING으로 남는다(대기 배지에 처리 불가능한 항목이 계속 쌓이던 문제 - #318).
    @Column(nullable = false)
    var isRead: Boolean = false,

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

    // IoT로 직접 제어할 수 없는 피드백(환기해주세요 등)에 대한 AI 조언(#296) - 온도 추천과
    // 상호 배타적(둘 다 채워지지 않음). 승인/거절 버튼은 이게 아니라 recommendedTemperature*
    // 유무로 노출 여부가 결정된다(제어할 대상이 있어야 승인이 의미 있음).
    @Column(columnDefinition = "TEXT")
    var aiAdvice: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun updateStatus(newStatus: FeedbackStatus) {
        status = newStatus
        // 승인/거절은 필연적으로 상세를 열어봤다는 뜻이라 읽음도 같이 세팅(일관성 보장) - #318
        isRead = true
    }

    fun markRead() {
        isRead = true
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

    fun applyAiAdvice(
        snapshotTemperature: Double?,
        snapshotHumidity: Double?,
        snapshotIlluminance: Int?,
        snapshotPeopleCount: Int?,
        advice: String,
    ) {
        this.snapshotTemperature = snapshotTemperature
        this.snapshotHumidity = snapshotHumidity
        this.snapshotIlluminance = snapshotIlluminance
        this.snapshotPeopleCount = snapshotPeopleCount
        this.aiAdvice = advice
    }
}
