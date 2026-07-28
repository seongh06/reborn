package com.reborn.server.domain.googlesheets

import com.reborn.server.domain.place.Place
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

@Entity
@Table(name = "google_sheets_credential")
class GoogleSheetsCredential(

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false, unique = true)
    val place: Place,

    @Column(nullable = false, length = 1024)
    var accessToken: String,

    @Column(nullable = false, length = 1024)
    var refreshToken: String,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun updateTokens(accessToken: String, refreshToken: String, expiresAt: LocalDateTime) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.expiresAt = expiresAt
    }
}
