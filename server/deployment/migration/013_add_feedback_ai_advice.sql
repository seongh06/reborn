-- ================================================
-- feedback 테이블에 AI 조언(ai_advice) 컬럼 추가 (#296)
--
-- 온도(더워요/추워요) 외 피드백은 IoT로 직접 제어할 수 없어 recommended_temperature_*를
-- 채우지 못하고 그냥 스킵됐었는데, 대신 Gemini가 생성한 "이런 조치를 해보세요" 식 조언
-- 텍스트를 저장한다. 온도 추천이 있는 피드백은 이 컬럼을 쓰지 않는다(둘은 상호 배타적).
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 013_add_feedback_ai_advice.sql
-- ================================================

ALTER TABLE `feedback`
    ADD COLUMN `ai_advice` TEXT NULL COMMENT 'IoT 제어 불가 피드백에 대한 AI 조언 텍스트' AFTER `recommended_temperature_after`;

-- 적용 후 확인
-- SHOW CREATE TABLE feedback\G
