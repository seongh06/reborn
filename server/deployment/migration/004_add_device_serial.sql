-- ================================================
-- 판매용 기기(Arduino/AI 스피커) 사전 발급 시리얼 재고 테이블 추가 (#147)
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 신규 테이블도 수동으로 적용해야 한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 004_add_device_serial.sql
-- ================================================

CREATE TABLE IF NOT EXISTS `device_serial`
(
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '시리얼 PK',
    `serial`             VARCHAR(8)  NOT NULL COMMENT '8자리 시리얼(앞 2자리 타입 프리픽스 AR/AI)',
    `device_type`        VARCHAR(20) NOT NULL COMMENT '시리얼이 발급된 기기 타입 (ARDUINO / AI_SPEAKER)',
    `assigned_device_id` BIGINT      NULL COMMENT '등록 완료 시 연결된 device.id (미할당이면 NULL)',
    `assigned_at`        DATETIME(6) NULL COMMENT '등록(할당) 시각',
    `created_at`         DATETIME(6) NOT NULL COMMENT '발급일시',
    `updated_at`         DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_serial_serial` (`serial`),
    UNIQUE KEY `uk_device_serial_assigned_device` (`assigned_device_id`),
    CONSTRAINT `fk_device_serial_device`
        FOREIGN KEY (`assigned_device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '판매용 기기 사전 발급 시리얼 재고';

-- 적용 후 확인
-- SHOW CREATE TABLE device_serial\G
