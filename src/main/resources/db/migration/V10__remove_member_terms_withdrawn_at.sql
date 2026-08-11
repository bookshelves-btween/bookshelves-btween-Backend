-- 약관 철회 기능이 없어 한 번도 값이 채워진 적 없는 죽은 컬럼이라 제거한다.
ALTER TABLE member_terms
    DROP COLUMN withdrawn_at;
