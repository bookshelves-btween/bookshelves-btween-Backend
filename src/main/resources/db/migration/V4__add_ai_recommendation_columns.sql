-- 오늘의 추천 도서에 필요한 컬럼을 채운다.
--
-- 베이스라인의 ai_recommendation은 book_id만 있는 스캐폴딩이라 어떤 책을 언제 노출할지 알 수 없었다.
--
-- 노출 날짜를 created_at에서 유도하지 않고 별도 컬럼으로 두는 이유는 스케줄러가 전날 23시에 미리
-- 만들기 때문이다. 생성 시각의 날짜와 노출 날짜가 하루 어긋난다.
--
-- unique 제약은 스케줄러가 중복 실행되거나 배포로 두 번 뜨더라도 하루에 두 권이 쌓이지 않게 한다.
-- 조회 시 하루치가 한 행이라는 것을 코드가 아니라 DB가 보장해야 홈 조회에서 어느 쪽을 고를지
-- 고민할 필요가 없어진다.
--
-- 기존 행은 없다. 스케줄러가 아직 없어 이 테이블에 쓴 적이 없으므로 NOT NULL을 바로 걸 수 있다.

ALTER TABLE ai_recommendation
    ADD COLUMN recommendation_message VARCHAR(300) NOT NULL,
    ADD COLUMN recommended_date DATE NOT NULL,
    ADD CONSTRAINT uk_ai_recommendation_date UNIQUE (recommended_date);
