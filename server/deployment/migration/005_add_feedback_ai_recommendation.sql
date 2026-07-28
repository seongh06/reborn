-- ================================================
-- feedback 테이블에 "AI 맞춤 피드백"(제출 시점 센서 스냅샷 + 추천 희망 온도) 컬럼 추가
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 컬럼 변경도 수동으로 적용해야 한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 005_add_feedback_ai_recommendation.sql
-- ================================================

ALTER TABLE `feedback`
    ADD COLUMN `snapshot_temperature` DOUBLE NULL COMMENT 'AI 추천 계산 시점 온도 스냅샷' AFTER `status`,
    ADD COLUMN `snapshot_humidity` DOUBLE NULL COMMENT 'AI 추천 계산 시점 습도 스냅샷' AFTER `snapshot_temperature`,
    ADD COLUMN `snapshot_illuminance` INT NULL COMMENT 'AI 추천 계산 시점 조도 스냅샷' AFTER `snapshot_humidity`,
    ADD COLUMN `snapshot_people_count` INT NULL COMMENT 'AI 추천 계산 시점 재실 인원 스냅샷' AFTER `snapshot_illuminance`,
    ADD COLUMN `recommended_temperature_before` DOUBLE NULL COMMENT 'AI 추천 전 온도(스냅샷과 동일)' AFTER `snapshot_people_count`,
    ADD COLUMN `recommended_temperature_after` DOUBLE NULL COMMENT 'Gemini가 추천한 희망 온도' AFTER `recommended_temperature_before`;

-- 적용 후 확인
-- SHOW CREATE TABLE feedback\G
