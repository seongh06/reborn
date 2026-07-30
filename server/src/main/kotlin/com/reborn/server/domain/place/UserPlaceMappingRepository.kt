package com.reborn.server.domain.place

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserPlaceMappingRepository : JpaRepository<UserPlaceMapping, Long> {

    fun findByUserIdAndPlaceId(userId: Long, placeId: Long): UserPlaceMapping?

    // 권한 확인만 필요한 곳(requireAdmin)에서 UserPlaceMapping을 엔티티로 통째로 로드하면,
    // 그 엔티티가 들고 있는 Place 연관관계가 같은 트랜잭션 내 다른 flush(예: 장소 삭제)의
    // Hibernate 캐스케이드 검증에 얽혀 TransientObjectException을 유발하는 문제가 있었다(#118).
    // accessLevel 값만 스칼라로 조회해 엔티티를 영속성 컨텍스트에 아예 올리지 않는다.
    @Query("SELECT m.accessLevel FROM UserPlaceMapping m WHERE m.user.id = :userId AND m.place.id = :placeId")
    fun findAccessLevelByUserIdAndPlaceId(@Param("userId") userId: Long, @Param("placeId") placeId: Long): AccessLevel?

    fun findAllByUserId(userId: Long): List<UserPlaceMapping>

    fun findAllByPlaceId(placeId: Long): List<UserPlaceMapping>

    // user는 LAZY라 관리자 여러 명을 순회하며 각자의 user를 참조하면 N+1이 발생한다(getAdmins,
    // withdraw의 sole-admin 체크 등 - CodeRabbit #218) - user를 같은 쿼리에서 함께 로드한다.
    @EntityGraph(attributePaths = ["user"])
    fun findAllByPlaceIdAndAccessLevel(placeId: Long, accessLevel: AccessLevel): List<UserPlaceMapping>

    fun findAllByUserIdAndAccessLevel(userId: Long, accessLevel: AccessLevel): List<UserPlaceMapping>

    fun existsByUserIdAndPlaceId(userId: Long, placeId: Long): Boolean

    fun deleteByUserIdAndPlaceId(userId: Long, placeId: Long)

    // 방장 여부만 필요한 곳(requireOwner)에서 엔티티를 통째로 로드하면 #118과 같은 이유로
    // TransientObjectException 위험이 있어, isOwner만 스칼라로 조회한다.
    @Query("SELECT m.isOwner FROM UserPlaceMapping m WHERE m.user.id = :userId AND m.place.id = :placeId")
    fun findIsOwnerByUserIdAndPlaceId(@Param("userId") userId: Long, @Param("placeId") placeId: Long): Boolean?
}
