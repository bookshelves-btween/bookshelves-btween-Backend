-- 전체 스키마 베이스라인 (#148)
--
-- 이 파일 하나가 빈 데이터베이스에 전체 스키마를 세운다. 운영은 ddl-auto=validate로 동작하므로
-- Hibernate가 테이블을 만들지 않는다. 스키마를 만드는 주체는 Flyway뿐이다.
--
-- 생성 방법: MySQL 8.4 컨테이너에 ddl-auto=create로 애플리케이션을 한 번 띄운 뒤 mysqldump로 추출하고,
-- Hibernate가 붙인 무작위 제약 이름을 읽을 수 있는 이름으로 바꿨다. 컬럼 정의는 추출본 그대로다.
--
-- 외래키는 테이블을 모두 만든 뒤 일괄로 건다. 생성 순서에 의존하지 않게 하기 위함이다.
-- 엔티티가 바뀌면 이 파일을 고치지 말고 새 마이그레이션을 추가한다.

-- ============================================================
-- 테이블
-- ============================================================

CREATE TABLE `ai_question` (
  `question_order` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_question_meeting_question_order` (`meeting_id`,`question_order`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `airecommendation` (
  `book_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `book` (
  `published_date` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `isbn` varchar(20) NOT NULL,
  `kdc_code` varchar(20) DEFAULT NULL,
  `kdc_name` varchar(100) DEFAULT NULL,
  `cover_image_url` varchar(500) DEFAULT NULL,
  `author` varchar(255) DEFAULT NULL,
  `description` text,
  `publisher` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_isbn` (`isbn`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `category` (
  `kdc_code` varchar(3) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_kdc_code` (`kdc_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `chat_message` (
  `chatroom_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sender_member_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `message` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `chat_room` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_room_meeting` (`meeting_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `device_token` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `fcm_token` varchar(255) NOT NULL,
  `platform` enum('IOS') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_token_fcm_token` (`fcm_token`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `meeting` (
  `cur_participants` int NOT NULL,
  `current_question_order` int NOT NULL DEFAULT '0',
  `duration` int NOT NULL,
  `max_participants` int NOT NULL,
  `real_participants` int NOT NULL,
  `book_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `start_date` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `status` enum('COMPLETED','IN_PROGRESS','RECRUITING','RECRUIT_CLOSED') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `meeting_participant` (
  `attended` bit(1) DEFAULT NULL,
  `is_leader` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meeting_participant_meeting_member` (`meeting_id`,`member_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `meeting_summary` (
  `ai_question_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `content` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_meeting_summary_ai_question` (`ai_question_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `member` (
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `nickname_animal` varchar(30) DEFAULT NULL,
  `nickname_modifier` varchar(30) DEFAULT NULL,
  `nickname_noun` varchar(30) DEFAULT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `provider_id` varchar(255) DEFAULT NULL,
  `profile_background_color` enum('BLUE','BROWN','GREEN','PURPLE','RED','YELLOW') DEFAULT NULL,
  `provider` enum('APPLE','GOOGLE','KAKAO') DEFAULT NULL,
  `status` enum('ACTIVE','ANONYMIZED','PENDING_ONBOARDING','SUSPENDED','WITHDRAWN') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_provider_provider_id` (`provider`,`provider_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `member_book` (
  `progress` int NOT NULL,
  `rating` decimal(2,1) DEFAULT NULL,
  `book_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `finished_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `memo` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_book_member_book` (`member_id`,`book_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `member_book_history` (
  `progress` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_book_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `member_category` (
  `category_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_category_member_category` (`member_id`,`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `member_terms` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `terms_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `withdrawn_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_terms_member_terms` (`member_id`,`terms_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `no_show_event` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meeting_id` bigint NOT NULL,
  `member_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `notification` (
  `is_read` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `related_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content` varchar(500) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('MEETING_CANCELED','MEETING_STARTED','MEETING_SUMMARY_DONE','SYSTEM') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `report` (
  `chatroom_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reporter_member_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `status` enum('PENDING','REJECTED','RESOLVED','REVIEWING') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_reporter_chatroom` (`reporter_member_id`,`chatroom_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `terms` (
  `is_required` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `version` varchar(20) NOT NULL,
  `content` text NOT NULL,
  `title` varchar(255) NOT NULL,
  `type` enum('PRIVACY','SERVICE') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ============================================================
-- 외래키
-- ============================================================

ALTER TABLE `ai_question`
    ADD CONSTRAINT `fk_ai_question_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`);

ALTER TABLE `airecommendation`
    ADD CONSTRAINT `fk_airecommendation_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`);

ALTER TABLE `chat_message`
    ADD CONSTRAINT `fk_chat_message_chatroom` FOREIGN KEY (`chatroom_id`) REFERENCES `chat_room` (`id`);

ALTER TABLE `chat_message`
    ADD CONSTRAINT `fk_chat_message_sender_member` FOREIGN KEY (`sender_member_id`) REFERENCES `member` (`id`);

ALTER TABLE `chat_room`
    ADD CONSTRAINT `fk_chat_room_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`);

ALTER TABLE `device_token`
    ADD CONSTRAINT `fk_device_token_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

ALTER TABLE `meeting`
    ADD CONSTRAINT `fk_meeting_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`);

ALTER TABLE `meeting_participant`
    ADD CONSTRAINT `fk_meeting_participant_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

ALTER TABLE `meeting_participant`
    ADD CONSTRAINT `fk_meeting_participant_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`);

ALTER TABLE `meeting_summary`
    ADD CONSTRAINT `fk_meeting_summary_ai_question` FOREIGN KEY (`ai_question_id`) REFERENCES `ai_question` (`id`);

ALTER TABLE `member_book`
    ADD CONSTRAINT `fk_member_book_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

ALTER TABLE `member_book`
    ADD CONSTRAINT `fk_member_book_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`);

ALTER TABLE `member_book_history`
    ADD CONSTRAINT `fk_member_book_history_member_book` FOREIGN KEY (`member_book_id`) REFERENCES `member_book` (`id`);

ALTER TABLE `member_category`
    ADD CONSTRAINT `fk_member_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`);

ALTER TABLE `member_category`
    ADD CONSTRAINT `fk_member_category_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

ALTER TABLE `member_terms`
    ADD CONSTRAINT `fk_member_terms_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

ALTER TABLE `member_terms`
    ADD CONSTRAINT `fk_member_terms_terms` FOREIGN KEY (`terms_id`) REFERENCES `terms` (`id`);

ALTER TABLE `no_show_event`
    ADD CONSTRAINT `fk_no_show_event_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

ALTER TABLE `no_show_event`
    ADD CONSTRAINT `fk_no_show_event_meeting` FOREIGN KEY (`meeting_id`) REFERENCES `meeting` (`id`);

ALTER TABLE `notification`
    ADD CONSTRAINT `fk_notification_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`);

ALTER TABLE `report`
    ADD CONSTRAINT `fk_report_reporter_member` FOREIGN KEY (`reporter_member_id`) REFERENCES `member` (`id`);

ALTER TABLE `report`
    ADD CONSTRAINT `fk_report_chatroom` FOREIGN KEY (`chatroom_id`) REFERENCES `chat_room` (`id`);

-- ============================================================
-- 카테고리 마스터 (KDC 100단위 10개)
--
-- 선호 장르와 독서 통계 롤업의 기준 데이터다. 코드로 생성하지 않으므로 여기서 적재한다.
-- ============================================================

INSERT INTO `category` (`kdc_code`, `name`) VALUES
    ('000', '총류'),
    ('100', '철학'),
    ('200', '종교'),
    ('300', '사회과학'),
    ('400', '자연과학'),
    ('500', '기술과학'),
    ('600', '예술'),
    ('700', '언어'),
    ('800', '문학'),
    ('900', '역사');
