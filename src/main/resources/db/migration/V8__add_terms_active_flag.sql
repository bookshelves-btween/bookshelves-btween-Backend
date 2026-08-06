-- 약관을 개정해 새 버전 row를 추가해도 조회/필수 동의 검증이 타입별 최신(활성) 버전만
-- 대상으로 하도록 활성 여부 컬럼을 추가한다. 기존 row는 전부 현재 활성 버전이므로 기본값 true.
--
-- 이후 실제로 약관을 개정할 때는(V5와 마찬가지로 세션매니저를 통한 수동 운영 SQL):
--   1. 새 버전 row를 INSERT (is_active = 1)
--   2. 같은 type의 기존 row를 UPDATE terms SET is_active = 0 WHERE type = '...' AND is_active = 1
-- 두 단계 모두 해줘야 조회/검증이 새 버전으로 넘어간다. 1번만 하고 2번을 빠뜨리면 이번에
-- 고친 버그가 다시 재현된다.
ALTER TABLE terms
    ADD COLUMN is_active bit(1) NOT NULL DEFAULT b'1' AFTER is_required;
