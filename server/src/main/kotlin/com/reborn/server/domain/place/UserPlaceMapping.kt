package com.reborn.server.domain.place

import com.reborn.server.domain.auth.User
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
@Table(name = "user_place_mapping")
class UserPlaceMapping(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    val place: Place,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val accessLevel: AccessLevel,

    // 장소를 등록한 최초 관리자(혹은 위임받은 관리자) - 장소 하드 삭제, 방장 위임 등 파괴적
    // 작업은 방장만 할 수 있다. 장소당 정확히 1명이어야 하며 register()/transferOwner()가 보장.
    @Column(name = "is_owner", nullable = false)
    var isOwner: Boolean = false,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity() {

    fun assignOwner() {
        isOwner = true
    }

    fun revokeOwner() {
        isOwner = false
    }
}
