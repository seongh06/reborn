-- ================================================
-- ReBorn Database Schema
-- Based on JPA Entity definitions
--
-- ⚠️ CREATE TABLE IF NOT EXISTS 한계 (2026-07-10, #118):
-- 이 파일을 수정해도 이미 테이블이 존재하는 환경(특히 운영)에는 반영되지 않는다.
-- 운영 DB의 device/user_place_mapping 테이블이 과거 Hibernate ddl-auto로 생성된 채
-- 남아있어 device_type enum이 옛 값(KIOSK)에 고정되고 FK에 CASCADE가 빠져있던 사고가
-- 있었음 — server/deployment/migration/001_fix_device_place_schema_drift.sql로 수정.
-- 기존 테이블의 컬럼/제약을 바꾸는 변경은 이 파일 수정만으로 끝나지 않고,
-- server/deployment/migration/에 별도 ALTER 스크립트를 추가하고 운영에 수동 적용해야 한다.
-- ================================================

-- ------------------------------------------------
-- 1. user
-- refreshToken 제거 → Redis refresh:{userId} 관리
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 PK',
    `email`         VARCHAR(255) NULL COMMENT '소셜 이메일 (카카오는 이메일 동의 항목 미사용 심사로 NULL 가능)',
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
    `wifi_ssid`   VARCHAR(64)  NULL COMMENT '이 장소 기기들이 사용할 홈 WiFi SSID',
    `wifi_password` VARCHAR(64) NULL COMMENT '이 장소 기기들이 사용할 홈 WiFi 비밀번호(평문)',
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
    `is_owner`     TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '방장 여부 - 장소당 1명, 하드 삭제/방장 위임 권한',
    -- MySQL은 partial/filtered unique index를 지원하지 않아, is_owner=1인 행만 place_id를
    -- 노출하는 생성 컬럼(그 외엔 NULL - MySQL unique index는 NULL을 서로 다른 값으로 취급해
    -- 여러 개 허용)으로 "장소당 방장 1명"을 DB 레벨에서 강제한다. 애플리케이션에서 읽거나 쓸
    -- 필요 없음 - MySQL이 INSERT/UPDATE마다 자동 계산.
    `owner_place_id` BIGINT GENERATED ALWAYS AS (IF(`is_owner` = 1, `place_id`, NULL)) VIRTUAL,
    `created_at`   DATETIME(6) NOT NULL COMMENT '매핑일시',
    `updated_at`   DATETIME(6) NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_place` (`user_id`, `place_id`),
    UNIQUE KEY `uk_user_place_mapping_owner_place` (`owner_place_id`),
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
    `device_type` VARCHAR(20)  NOT NULL COMMENT '기기 유형 (ARDUINO / AEROMETER / SMART_THINGS / AI_SPEAKER)',
    `device_key`  VARCHAR(255) NOT NULL COMMENT '인증용 고유 키',
    `name`        VARCHAR(100) NULL COMMENT '기기 이름',
    `category`    VARCHAR(20)  NULL COMMENT '아이콘 구분용 카테고리 (LAMP/PLUG/TV/AIR_CONDITIONER/CURTAIN/OTHER) - SmartThings 등록 시 관리자가 직접 선택, Arduino/AI스피커는 항상 NULL',
    `app_token`   VARCHAR(512) NULL COMMENT 'FCM 앱 토큰 (AEROMETER 전용)',
    `is_online`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '온라인 여부',
    `has_ir_control` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'IR 송신 모듈 보유 여부 (ARDUINO 전용, #288)',
    `pending_ir_command` VARCHAR(20) NULL COMMENT '대기 중인 IR 명령 - 폴링 시 1회성 소비 (#288)',
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
    `device_id`     BIGINT        NULL COMMENT '기기 FK (기기 삭제 시 NULL, 애초에 기기 미지목 제출도 가능)',
    `place_id`      BIGINT        NOT NULL COMMENT '장소 FK - device가 없어도 항상 채워짐(목록/개수 조회 기준)',
    `content`       VARCHAR(1000) NOT NULL COMMENT '피드백 내용',
    `session_token` VARCHAR(255)  NULL COMMENT 'QR 접속 임시 세션 토큰 (음성 피드백은 NULL)',
    `user_agent`    VARCHAR(1024) NULL COMMENT '브라우저 User-Agent',
    `source`        VARCHAR(20)   NOT NULL DEFAULT 'QR' COMMENT '피드백 출처 (QR / VOICE)',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태 (PENDING / APPROVED / REJECTED)',
    `is_read`       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '관리자가 상세를 열어본 적 있는지',
    `snapshot_temperature`           DOUBLE NULL COMMENT 'AI 추천 계산 시점 온도 스냅샷',
    `snapshot_humidity`              DOUBLE NULL COMMENT 'AI 추천 계산 시점 습도 스냅샷',
    `snapshot_illuminance`           INT    NULL COMMENT 'AI 추천 계산 시점 조도 스냅샷',
    `snapshot_people_count`          INT    NULL COMMENT 'AI 추천 계산 시점 재실 인원 스냅샷',
    `recommended_temperature_before` DOUBLE NULL COMMENT 'AI 추천 전 온도(스냅샷과 동일)',
    `recommended_temperature_after`  DOUBLE NULL COMMENT 'Gemini가 추천한 희망 온도',
    `ai_advice`     TEXT          NULL COMMENT 'IoT 제어 불가 피드백에 대한 AI 조언 텍스트',
    `created_at`    DATETIME(6)   NOT NULL COMMENT '작성일시',
    `updated_at`    DATETIME(6)   NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    KEY `idx_feedback_device_status` (`device_id`, `status`) COMMENT '기기별 상태 필터 조회 최적화',
    KEY `idx_feedback_place_status` (`place_id`, `status`) COMMENT '장소별 상태 필터 조회 최적화(device 미지목 포함)',
    CONSTRAINT `fk_feedback_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_feedback_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '방문자 피드백';

-- ------------------------------------------------
-- 7. smart_things_credential (2026-07-19, #130)
-- 장소별 SmartThings OAuth 토큰. PKCE 미지원으로 client_secret이 서버에만
-- 있어야 해서, 서버가 이 토큰으로 SmartThings Cloud API를 직접 호출한다.
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `smart_things_credential`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '자격증명 PK',
    `place_id`      BIGINT       NOT NULL COMMENT '장소 FK (장소당 1개)',
    `access_token`  VARCHAR(1024) NOT NULL COMMENT 'SmartThings OAuth AccessToken',
    `refresh_token` VARCHAR(1024) NOT NULL COMMENT 'SmartThings OAuth RefreshToken',
    `expires_at`    DATETIME(6)  NOT NULL COMMENT 'AccessToken 만료 시각',
    `created_at`    DATETIME(6)  NOT NULL COMMENT '연동일시',
    `updated_at`    DATETIME(6)  NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_smart_things_credential_place` (`place_id`),
    CONSTRAINT `fk_smart_things_credential_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '장소별 SmartThings OAuth 자격증명';

-- ------------------------------------------------
-- 8. device_serial (2026-07-22, #147)
-- 판매용 Arduino/AI 스피커 실물에 인쇄할 시리얼 재고. place 매핑 전 상태를
-- 표현해야 해서 device(place_id NOT NULL)와 별도 테이블로 둔다.
-- ------------------------------------------------
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

-- ------------------------------------------------
-- 9. auto_control_rule (2026-07-28, #190)
-- 기기당 자동 제어 규칙 1건. 클라이언트가 자유 텍스트("28°C" 등)로 편집하는 값 그대로 저장하고,
-- 조건 평가 시점(스케줄러)에만 숫자를 파싱한다. 현재는 온도 상/하한 조건만 실제로 평가·실행되고
-- 나머지(습도/재실인원/불쾌지수/자동 꺼짐)는 저장만 되고 실행은 미구현(#190 범위 밖, 후속 이슈 필요).
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `auto_control_rule`
(
    `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '규칙 PK',
    `device_id`                 BIGINT       NOT NULL COMMENT '기기 FK (기기당 1개)',
    `discomfort_threshold`      VARCHAR(20)  NULL COMMENT '불쾌지수 기준값(자유 텍스트)',
    `discomfort_action`         VARCHAR(50)  NULL COMMENT '불쾌지수 초과 시 동작(자유 텍스트)',
    `humidity_high_threshold`   VARCHAR(20)  NULL COMMENT '습도 상한 기준값(자유 텍스트, 예: "70%")',
    `humidity_high_action`      VARCHAR(50)  NULL COMMENT '습도 상한 초과 시 동작',
    `humidity_low_threshold`    VARCHAR(20)  NULL COMMENT '습도 하한 기준값',
    `humidity_low_action`       VARCHAR(50)  NULL COMMENT '습도 하한 미만 시 동작',
    `temperature_high_threshold` VARCHAR(20) NULL COMMENT '온도 상한 기준값(자유 텍스트, 예: "28°C") - 실행됨',
    `temperature_high_action`   VARCHAR(50)  NULL COMMENT '온도 상한 초과 시 동작 - 실행됨',
    `temperature_low_threshold` VARCHAR(20)  NULL COMMENT '온도 하한 기준값 - 실행됨',
    `temperature_low_action`    VARCHAR(50)  NULL COMMENT '온도 하한 미만 시 동작 - 실행됨',
    `occupancy_threshold`       VARCHAR(20)  NULL COMMENT '재실 인원 기준값(자유 텍스트, 예: "5명")',
    `occupancy_action`          VARCHAR(50)  NULL COMMENT '재실 인원 초과 시 동작',
    `is_auto_off_enabled`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '자동 꺼짐 사용 여부',
    `auto_off_minutes`          VARCHAR(10)  NULL COMMENT '자동 꺼짐까지 대기 분(자유 텍스트)',
    `last_triggered_at`         DATETIME(6)  NULL COMMENT '마지막으로 조건이 실행된 시각(쿨다운 기준, 온도 조건 전용)',
    `created_at`                DATETIME(6)  NOT NULL COMMENT '등록일시',
    `updated_at`                DATETIME(6)  NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auto_control_rule_device` (`device_id`),
    CONSTRAINT `fk_auto_control_rule_device`
        FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '기기별 자동 제어 규칙';

-- ------------------------------------------------
-- 10. google_sheets_credential (2026-07-28, 데이터 화면 내보내기)
-- 장소별 Google Sheets OAuth 토큰. smart_things_credential과 동일 패턴 - 서버가
-- 토큰을 직접 보유하고 Sheets API를 호출해 데이터를 내보낸다.
-- ------------------------------------------------
CREATE TABLE IF NOT EXISTS `google_sheets_credential`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '자격증명 PK',
    `place_id`      BIGINT       NOT NULL COMMENT '장소 FK (장소당 1개)',
    `access_token`  VARCHAR(1024) NOT NULL COMMENT 'Google OAuth AccessToken',
    `refresh_token` VARCHAR(1024) NOT NULL COMMENT 'Google OAuth RefreshToken',
    `expires_at`    DATETIME(6)  NOT NULL COMMENT 'AccessToken 만료 시각',
    `created_at`    DATETIME(6)  NOT NULL COMMENT '연동일시',
    `updated_at`    DATETIME(6)  NOT NULL COMMENT '수정일시',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_google_sheets_credential_place` (`place_id`),
    CONSTRAINT `fk_google_sheets_credential_place`
        FOREIGN KEY (`place_id`) REFERENCES `place` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '장소별 Google Sheets OAuth 자격증명';

-- ------------------------------------------------
-- 11. api_request_log (2026-07-29, 릴리즈 사용 현황 분석 #222)
-- LoggingInterceptor가 매 요청마다 비동기로 기록. user_id는 FK가 아닌 단순 참조 컬럼 -
-- 고빈도 쓰기 테이블에 FK 오버헤드를 두지 않고, 탈퇴한 사용자의 과거 요청 이력도 보존한다.
-- ------------------------------------------------
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

-- ------------------------------------------------
-- 12. auto_control_execution_log (2026-07-29, 릴리즈 사용 현황 분석 #222)
-- AutoControlEvaluationService가 실제로 제어 명령을 보낼 때마다 기록. 전력계 연동이 없어
-- kWh 등 에너지 절감량 환산은 하지 않고 실행 횟수/내역만 축적한다.
-- ------------------------------------------------
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
