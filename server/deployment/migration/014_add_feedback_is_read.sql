-- ================================================
-- feedback 테이블에 is_read(읽음 여부) 컬럼 추가 (#318)
--
-- 기존엔 상태가 대기(PENDING)/승인(APPROVED)/거절(REJECTED) 3단계뿐이었는데, 온도 조절이 아닌
-- (승인/거절할 IoT 액션 자체가 없는) 피드백은 AI 조언만 받고 영원히 PENDING으로 남아 "대기" 배지가
-- 처리 불가능한 항목까지 계속 누적되는 문제가 있었다(#296/#297). status는 그대로 두고, "읽음 여부"를
-- 별도 축으로 분리한다 - 안읽음/읽음은 이 컬럼으로, 승인/거절은 기존 status로 판단(둘은 직교).
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 014_add_feedback_is_read.sql
-- ================================================

ALTER TABLE `feedback`
    ADD COLUMN `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '관리자가 상세를 열어본 적 있는지' AFTER `status`;

-- 이미 승인/거절된 피드백은 관리자가 필연적으로 열어봤을 것이므로 읽음으로 백필
UPDATE `feedback` SET `is_read` = 1 WHERE `status` IN ('APPROVED', 'REJECTED');

-- 적용 후 확인
-- SHOW CREATE TABLE feedback\G
