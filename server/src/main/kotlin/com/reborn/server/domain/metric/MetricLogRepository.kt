package com.reborn.server.domain.metric

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface MetricLogRepository : JpaRepository<MetricLog, Long> {

    fun findTopByDeviceIdOrderByCreatedAtDesc(deviceId: Long): MetricLog?

    fun findAllByDeviceId(deviceId: Long, pageable: Pageable): Page<MetricLog>

    @Query(
        "SELECT m FROM MetricLog m " +
            "WHERE m.device.id = :deviceId AND m.createdAt BETWEEN :from AND :to " +
            "ORDER BY m.createdAt DESC",
    )
    fun findAllByDeviceIdAndPeriod(
        @Param("deviceId") deviceId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<MetricLog>

    // 기기 삭제(ON DELETE SET NULL) 후 device_id = NULL 상태인 고아 로그 조회
    fun findAllByDeviceIsNull(pageable: Pageable): Page<MetricLog>

    // 주/월/년 집계(#158 데이터 화면 장기 차트) — 날짜 절삭(GROUP BY) 표현이 JPQL로는 이식성 있게
    // 안 되는 영역이라 네이티브 SQL 사용. 시작 시점 없이 기기의 전체 이력을 묶고, 서비스 계층에서
    // 최근 N개 버킷만 잘라 쓴다(장기 보관되는 로그 양이 많지 않아 성능상 문제 없음).
    @Query(
        value = "SELECT CAST(YEARWEEK(created_at, 3) AS CHAR) AS bucketKey, " +
            "AVG(temperature) AS avgTemperature, AVG(humidity) AS avgHumidity, " +
            "AVG(illuminance) AS avgIlluminance, AVG(occupancy) AS avgPeopleCount " +
            "FROM metric_logs WHERE device_id = :deviceId " +
            "GROUP BY bucketKey ORDER BY bucketKey ASC",
        nativeQuery = true,
    )
    fun findWeeklyAggregates(@Param("deviceId") deviceId: Long): List<MetricAggregateProjection>

    @Query(
        value = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS bucketKey, " +
            "AVG(temperature) AS avgTemperature, AVG(humidity) AS avgHumidity, " +
            "AVG(illuminance) AS avgIlluminance, AVG(occupancy) AS avgPeopleCount " +
            "FROM metric_logs WHERE device_id = :deviceId " +
            "GROUP BY bucketKey ORDER BY bucketKey ASC",
        nativeQuery = true,
    )
    fun findMonthlyAggregates(@Param("deviceId") deviceId: Long): List<MetricAggregateProjection>

    @Query(
        value = "SELECT CAST(YEAR(created_at) AS CHAR) AS bucketKey, " +
            "AVG(temperature) AS avgTemperature, AVG(humidity) AS avgHumidity, " +
            "AVG(illuminance) AS avgIlluminance, AVG(occupancy) AS avgPeopleCount " +
            "FROM metric_logs WHERE device_id = :deviceId " +
            "GROUP BY bucketKey ORDER BY bucketKey ASC",
        nativeQuery = true,
    )
    fun findYearlyAggregates(@Param("deviceId") deviceId: Long): List<MetricAggregateProjection>
}

interface MetricAggregateProjection {
    fun getBucketKey(): String
    fun getAvgTemperature(): Double?
    fun getAvgHumidity(): Double?
    fun getAvgIlluminance(): Double?
    fun getAvgPeopleCount(): Double?
}
