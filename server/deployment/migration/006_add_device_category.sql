-- ================================================
-- device 테이블에 아이콘 구분용 category 컬럼 추가
--
-- 배경: 운영 SQL_INIT_MODE=never라 schema.sql은 운영 DB에 자동 적용되지
-- 않는다(#118에서 확인된 정책). 컬럼 변경도 수동으로 적용해야 한다.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 006_add_device_category.sql
-- ================================================

ALTER TABLE `device`
    ADD COLUMN `category` VARCHAR(20) NULL COMMENT '아이콘 구분용 카테고리 (LAMP/PLUG/TV/AIR_CONDITIONER/CURTAIN/OTHER) - SmartThings 등록 시 관리자가 직접 선택, Arduino/AI스피커는 항상 NULL' AFTER `name`;

-- 적용 후 확인
-- SHOW CREATE TABLE device\G
