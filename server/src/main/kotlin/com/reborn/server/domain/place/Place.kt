package com.reborn.server.domain.place

import com.reborn.server.global.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
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

    @Column
    val description: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

) : BaseEntity()
