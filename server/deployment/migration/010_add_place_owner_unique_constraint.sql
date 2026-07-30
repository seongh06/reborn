-- ================================================
-- user_place_mapping에 "장소당 방장 1명" DB 레벨 제약 추가
--
-- 배경: 009에서 is_owner 컬럼을 추가했지만 서비스 레이어 로직만으로 유일성을
-- 보장하고 있었음(CodeRabbit #258 리뷰) - 버그나 동시성 문제로 한 장소에
-- 방장이 2명 이상 생기는 걸 DB가 막지 못했다.
--
-- MySQL은 partial/filtered unique index를 지원하지 않아, is_owner=1인 행만
-- place_id를 노출하는 생성 컬럼(그 외엔 NULL)에 유니크 인덱스를 걸어 우회한다.
-- MySQL의 unique index는 NULL을 서로 다른 값으로 취급해 여러 개 허용하므로,
-- is_owner=0인 행끼리는 충돌하지 않는다.
--
-- 사전 조건: 009 마이그레이션이 이미 적용되어 있어야 하고, 장소별 is_owner=1이
-- 정확히 1건씩이어야 한다(009가 이미 백필로 보장) - 아래 확인 쿼리로 먼저 검증.
--
-- 실행: docker exec -i reborn-mysql sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -u root reborn' < 010_add_place_owner_unique_constraint.sql
-- ================================================

-- 적용 전 검증 - 결과가 있으면 먼저 데이터를 정리한 뒤에 이 마이그레이션을 실행할 것
-- SELECT place_id, COUNT(*) c FROM user_place_mapping WHERE is_owner = 1 GROUP BY place_id HAVING c <> 1;

ALTER TABLE `user_place_mapping`
    ADD COLUMN `owner_place_id` BIGINT GENERATED ALWAYS AS (IF(`is_owner` = 1, `place_id`, NULL)) VIRTUAL AFTER `is_owner`,
    ADD UNIQUE KEY `uk_user_place_mapping_owner_place` (`owner_place_id`);

-- 적용 후 확인
-- SHOW CREATE TABLE user_place_mapping\G
