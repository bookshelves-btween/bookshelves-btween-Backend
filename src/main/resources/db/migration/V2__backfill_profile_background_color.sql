-- profile_background_color는 Hibernate ddl-auto가 MySQL 네이티브 ENUM 컬럼으로 생성해뒀다.
-- 네이티브 ENUM은 Java enum이 바뀌어도 컬럼 자체의 값 목록이 자동으로 갱신되지 않고,
-- 목록에 없는 값을 쓰려 하면 그냥 거부(데이터 손실 없이 에러)된다.
-- 그래서 옛 값+새 값을 모두 허용하도록 넓힌 뒤 데이터를 백필하고, 마지막에 새 목록으로 좁힌다.

-- 1단계: 기존 값(BLACK/BLUE/GREEN/ORANGE/PINK/PURPLE) + 새 값(BROWN/RED/YELLOW) 모두 허용
ALTER TABLE member MODIFY COLUMN profile_background_color
    ENUM('BLACK', 'BLUE', 'GREEN', 'ORANGE', 'PINK', 'PURPLE', 'BROWN', 'RED', 'YELLOW');

-- 2단계: 제거되는 값을 새 팔레트에서 가장 가까운 색으로 백필
UPDATE member SET profile_background_color = 'YELLOW' WHERE profile_background_color = 'ORANGE';
UPDATE member SET profile_background_color = 'RED' WHERE profile_background_color = 'PINK';
UPDATE member SET profile_background_color = 'PURPLE' WHERE profile_background_color = 'BLACK';

-- 3단계: 이제 새 팔레트 값만 남았으니 컬럼을 새 목록으로 좁힌다
ALTER TABLE member MODIFY COLUMN profile_background_color
    ENUM('BROWN', 'PURPLE', 'BLUE', 'GREEN', 'RED', 'YELLOW');
