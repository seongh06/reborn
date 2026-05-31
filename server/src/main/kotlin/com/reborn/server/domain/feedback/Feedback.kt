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
    @JoinColumn(name = "device_id", nullable = false)
    val device: Device,

    @Column(nullable = false, length = 1000)
    val content: String,

    @Column(nullable = false)
    val sessionToken: String,

    @Column
    val userAgent: String? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun updateStatus(newStatus: FeedbackStatus) {
        status = newStatus
    }
}
