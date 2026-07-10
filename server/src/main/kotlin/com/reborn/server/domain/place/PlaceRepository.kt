package com.reborn.server.domain.place

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PlaceRepository : JpaRepository<Place, Long> {

    fun findByQrCode(qrCode: String): Place?

    fun existsByQrCode(qrCode: String): Boolean

    // JpaRepository.delete(entity)는 영속성 컨텍스트에 함께 로드된 다른 엔티티(예: 삭제 권한
    // 확인 중 조회한 UserPlaceMapping)와의 연관관계 캐스케이드 검증 과정에서 운영 환경에서만
    // TransientObjectException을 던지는 문제가 있어(#118), 엔티티 그래프를 거치지 않는
    // JPQL bulk delete로 우회한다. DB의 ON DELETE CASCADE/SET NULL이 자식 행 정리를 담당한다.
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Place p WHERE p.id = :id")
    fun deleteByIdInBulk(@Param("id") id: Long)
}
