-- ================================================
-- feedback 테이블에 음성 피드백(AI 스피커, #142) 지원 컬럼 추가
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 컬럼 변경도 수동으로 적용해야 한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 003_feedback_voice_source.sql
-- ================================================

-- QR 피드백은 session_token이 필수였지만, 기기(AI 스피커)발 피드백은 세션이 없으므로 nullable로 완화.
ALTER TABLE `feedback`
    MODIFY COLUMN `session_token` VARCHAR(255) NULL COMMENT 'QR 접속 임시 세션 토큰 (음성 피드백은 NULL)';

-- 피드백 출처 구분 (QR 웹 / 음성 AI 스피커). 관리자 앱에서 구분 표시용.
ALTER TABLE `feedback`
    ADD COLUMN `source` VARCHAR(20) NOT NULL DEFAULT 'QR' COMMENT '피드백 출처 (QR / VOICE)' AFTER `user_agent`;

-- device.device_type은 VARCHAR(20)이라 AI_SPEAKER 값 추가에 스키마 변경 불필요.

-- 적용 후 확인
-- SHOW CREATE TABLE feedback\G
