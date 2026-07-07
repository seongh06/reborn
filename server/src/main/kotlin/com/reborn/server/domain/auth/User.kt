package com.reborn.server.domain.auth

import com.reborn.server.global.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user",
    uniqueConstraints = [UniqueConstraint(name = "uk_user_provider_provider_id", columnNames = ["provider", "provider_id"])],
)
class User(

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val name: String,

    @Column
    val profileImage: String? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val provider: OAuthProvider,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    @Column(name = "fcm_token")
    var fcmToken: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun updateFcmToken(token: String?) {
        fcmToken = token
    }
}
