-- ================================================
-- device_schedule_rule 테이블 신설 (시간 기반 자동제어 #325)
--
-- auto_control_rule(#190, 센서 조건 기반)과 별개 축. 기기당 여러 건 등록 가능(예: "07시 켜짐" +
-- "22시 꺼짐"을 한 기기에 동시에 둘 수 있어야 해서 device_id에 UNIQUE를 걸지 않는다). SmartThings
-- 기기만 대상 - 서버가 자격증명을 직접 들고 있어 스케줄러가 동기 호출 가능. ARDUINO(IR)는 대기열
-- 적재만 가능해 실제 실행 시점이 폴링 주기에 달려있어 이번 스코프에서는 제외.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 015_add_device_schedule_rule.sql
-- ================================================

CREATE TABLE IF NOT EXISTS `device_schedule_rule`
(
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '규칙 PK',
    `device_id`          BIGINT      NOT NULL COMMENT '기기 FK (기기당 여러 건 가능)',
    `hour`               INT         NOT NULL COMMENT '실행 시(0~23)',
    `minute`             INT         NOT NULL COMMENT '실행 분(0~59)',
    `days_of_week`       VARCHAR(60) NOT NULL COMMENT '실행 요일 CSV (예: MONDAY,WEDNESDAY,FRIDAY)',
    `is_power_on`        TINYINT(1)  NOT NULL COMMENT '실행 시 전원 상태',
    `operation_mode`     VARCHAR(20) NULL COMMENT '실행 시 운전모드(선택, null이면 전원만 반영)',
    `enabled`            TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '활성화 여부(끄면 스케줄러가 건너뜀)',
    `last_triggered_at`  DATETIME(6) NULL COMMENT '같은 분에 중복 실행 방지용 마지막 실행 시각',
    `created_at`         DATETIME(6) NOT NULL COMMENT '등록일시',
    `updated_at`         DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    KEY `idx_device_schedule_rule_device` (`device_id`) COMMENT '기기별 규칙 목록 조회 최적화',
    KEY `idx_device_schedule_rule_enabled_time` (`enabled`, `hour`, `minute`) COMMENT '스케줄러 매분 평가 조회 최적화',
    CONSTRAINT `fk_device_schedule_rule_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '기기별 시간 기반 자동제어 규칙';

-- 적용 후 확인
-- SHOW CREATE TABLE device_schedule_rule\G
