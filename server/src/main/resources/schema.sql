-- ================================================
-- ReBorn Database Schema
-- Based on JPA Entity definitions
-- ================================================

-- ------------------------------------------------
-- 1. user
-- refreshToken 제거 → Redis refresh:{userId} 관리
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 PK',
    `email`         VARCHAR(255) NOT NULL COMMENT '소셜 이메일',
    `name`          VARCHAR(100) NOT NULL COMMENT '사용자 이름',
    `profile_image` VARCHAR(512) NULL COMMENT '프로필 이미지 URL',
    `provider`      VARCHAR(20)  NOT NULL COMMENT 'OAuth 제공자 (KAKAO / GOOGLE)',
    `provider_id`   VARCHAR(255) NOT NULL COMMENT '소셜 제공자 고유 사용자 ID (구글 sub, 카카오 id)',
    `fcm_token`     VARCHAR(255) NULL COMMENT 'FCM 푸시 토큰',
    `created_at`    DATETIME(6)  NOT NULL COMMENT '가입일시',
    `updated_at`    DATETIME(6)  NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`),
    UNIQUE KEY `uk_user_provider_provider_id` (`provider`, `provider_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자';

-- ------------------------------------------------
-- 2. place
-- qr_code UNIQUE 추가 → QR 장소 식별용
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `place`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '장소 PK',
    `name`        VARCHAR(255) NOT NULL COMMENT '장소명',
    `qr_code`     VARCHAR(255) NOT NULL COMMENT 'QR 코드 식별자',
    `type`        VARCHAR(20)  NOT NULL COMMENT '공간 유형 (HOME / STORE / COMPANY)',
    `description` VARCHAR(255) NULL COMMENT '장소 설명',
    `created_at`  DATETIME(6)  NOT NULL COMMENT '등록일시',
    `updated_at`  DATETIME(6)  NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_place_qr_code` (`qr_code`),
    CONSTRAINT `chk_place_type` CHECK (`type` IN ('HOME', 'STORE', 'COMPANY'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '장소';

-- ------------------------------------------------
-- 3. user_place_mapping
-- (user_id, place_id) UNIQUE → 중복 매핑 방지
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_place_mapping`
(
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '매핑 PK',
    `user_id`      BIGINT      NOT NULL COMMENT '사용자 FK',
    `place_id`     BIGINT      NOT NULL COMMENT '장소 FK',
    `access_level` VARCHAR(10) NOT NULL COMMENT '권한 (ADMIN / USER)',
    `created_at`   DATETIME(6) NOT NULL COMMENT '매핑일시',
    `updated_at`   DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_place` (`user_id`, `place_id`),
    CONSTRAINT `fk_upm_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_upm_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '사용자-장소 권한 매핑';

-- ------------------------------------------------
-- 4. device
-- device_key UNIQUE → Arduino/Kiosk 인증 식별자
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `device`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '기기 PK',
    `place_id`    BIGINT       NOT NULL COMMENT '장소 FK',
    `device_type` VARCHAR(20)  NOT NULL COMMENT '기기 유형 (ARDUINO / AEROMETER)',
    `device_key`  VARCHAR(255) NOT NULL COMMENT '인증용 고유 키',
    `name`        VARCHAR(100) NULL COMMENT '기기 이름',
    `app_token`   VARCHAR(512) NULL COMMENT 'FCM 앱 토큰 (AEROMETER 전용)',
    `is_online`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '온라인 여부',
    `created_at`  DATETIME(6)  NOT NULL COMMENT '등록일시',
    `updated_at`  DATETIME(6)  NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_key` (`device_key`),
    CONSTRAINT `fk_device_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '기기 (Arduino / Aerometer)';

-- ------------------------------------------------
-- 5. metric_logs
-- 복합 인덱스 (device_id, created_at DESC) → 최신 로그 조회 최적화
-- device_id nullable + SET NULL → 기기 삭제 시 로그 데이터 보존
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `metric_logs`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '로그 PK',
    `device_id`   BIGINT      NULL COMMENT '기기 FK (기기 삭제 시 NULL)',
    `temperature` DOUBLE      NULL COMMENT '온도 (°C)',
    `humidity`    DOUBLE      NULL COMMENT '습도 (%)',
    `illuminance` INT         NULL COMMENT '조도 (lux)',
    `occupancy`   INT         NULL COMMENT '재실 인원',
    `created_at`  DATETIME(6) NOT NULL COMMENT '수집일시',
    `updated_at`  DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    KEY `idx_metric_device_created` (`device_id`, `created_at` DESC) COMMENT '기기별 최신 로그 조회 최적화',
    CONSTRAINT `fk_metric_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '메트릭 수집 로그';

-- ------------------------------------------------
-- 6. feedback
-- user_agent VARCHAR(1024) → 긴 User-Agent 수용
-- device_id nullable + SET NULL → 기기 삭제 시 피드백 데이터 보존
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `feedback`
(
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '피드백 PK',
    `device_id`     BIGINT        NULL COMMENT '기기 FK (기기 삭제 시 NULL)',
    `content`       VARCHAR(1000) NOT NULL COMMENT '피드백 내용',
    `session_token` VARCHAR(255)  NOT NULL COMMENT 'QR 접속 임시 세션 토큰',
    `user_agent`    VARCHAR(1024) NULL COMMENT '브라우저 User-Agent',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태 (PENDING / APPROVED / REJECTED)',
    `created_at`    DATETIME(6)   NOT NULL COMMENT '작성일시',
    `updated_at`    DATETIME(6)   NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    KEY `idx_feedback_device_status` (`device_id`, `status`) COMMENT '기기별 상태 필터 조회 최적화',
    CONSTRAINT `fk_feedback_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '방문자 피드백';
