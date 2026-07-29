-- ================================================
-- API 요청 로그 + 자동 제어 실행 이력 테이블 추가 (릴리즈 사용 현황 분석 #222)
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 신규 테이블도 수동으로 적용해야 한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 008_add_analytics_tables.sql
-- ================================================

CREATE TABLE IF NOT EXISTS `api_request_log`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    `method`     VARCHAR(10) NOT NULL COMMENT 'HTTP 메서드',
    `path`       VARCHAR(255) NOT NULL COMMENT '요청 경로',
    `status`     INT         NOT NULL COMMENT 'HTTP 응답 상태 코드',
    `user_id`    BIGINT      NULL COMMENT '인증된 사용자 ID (비인증 요청이면 NULL, FK 아님)',
    `elapsed_ms` BIGINT      NOT NULL COMMENT '처리 소요 시간(ms)',
    `created_at` DATETIME(6) NOT NULL COMMENT '요청일시',
    `updated_at` DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    KEY `idx_api_request_log_created` (`created_at`) COMMENT '기간별 집계 조회 최적화',
    KEY `idx_api_request_log_user` (`user_id`) COMMENT '사용자별 활동 조회 최적화'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'API 요청 로그 (릴리즈 사용 현황 분석용)';

CREATE TABLE IF NOT EXISTS `auto_control_execution_log`
(
    `id`                    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    `device_id`             BIGINT      NULL COMMENT '기기 FK (기기 삭제 시 NULL, 이력 보존)',
    `triggered_temperature` DOUBLE      NOT NULL COMMENT '트리거 시점 온도',
    `action`                VARCHAR(50) NOT NULL COMMENT '실행된 액션(자유 텍스트 프리셋 - 냉방 시작 등)',
    `created_at`            DATETIME(6) NOT NULL COMMENT '실행일시',
    `updated_at`            DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    KEY `idx_auto_control_execution_log_device` (`device_id`, `created_at` DESC) COMMENT '기기별 실행 이력 조회 최적화',
    CONSTRAINT `fk_auto_control_execution_log_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '자동 제어 실행 이력 (릴리즈 사용 현황 분석용)';

-- 적용 후 확인
-- SHOW CREATE TABLE api_request_log\G
-- SHOW CREATE TABLE auto_control_execution_log\G
