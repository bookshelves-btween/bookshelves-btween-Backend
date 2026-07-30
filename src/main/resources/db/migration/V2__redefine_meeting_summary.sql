-- 모임 요약을 질문당 1행에서 분석 축 3개 기반 3주제 구조로 재정의한다.
--
-- 운영은 ddl-auto=validate로 동작하므로 Hibernate가 테이블을 만들지 않는다. 마이그레이션이
-- 컬럼 정의를 직접 갖는다. created_at과 updated_at은 BaseEntity가 NOT NULL로 요구하므로
-- 빠뜨리면 스키마 검증이 실패한다.
--
-- 생성 코드가 존재한 적이 없어 기존 테이블은 비어 있다.

DROP TABLE IF EXISTS meeting_summary;

CREATE TABLE meeting_summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meeting_id BIGINT NOT NULL,
    axis ENUM('KEY_ARGUMENT', 'REACTION', 'LIFE_LINK') NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meeting_summary_meeting_axis UNIQUE (meeting_id, axis),
    CONSTRAINT fk_meeting_summary_meeting FOREIGN KEY (meeting_id) REFERENCES meeting (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
