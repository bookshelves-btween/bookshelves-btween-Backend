CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

SET @kdc_code_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'category'
      AND column_name = 'kdc_code'
);

SET @add_kdc_code_sql = IF(
    @kdc_code_column_exists = 0,
    'ALTER TABLE category ADD COLUMN kdc_code VARCHAR(3) NULL AFTER id',
    'SELECT 1'
);

PREPARE add_kdc_code_statement FROM @add_kdc_code_sql;
EXECUTE add_kdc_code_statement;
DEALLOCATE PREPARE add_kdc_code_statement;

UPDATE category
SET kdc_code = CASE name
    WHEN '총류' THEN '000'
    WHEN '철학' THEN '100'
    WHEN '종교' THEN '200'
    WHEN '사회과학' THEN '300'
    WHEN '자연과학' THEN '400'
    WHEN '기술과학' THEN '500'
    WHEN '예술' THEN '600'
    WHEN '언어' THEN '700'
    WHEN '문학' THEN '800'
    WHEN '역사' THEN '900'
END
WHERE kdc_code IS NULL
  AND name IN ('총류', '철학', '종교', '사회과학', '자연과학', '기술과학', '예술', '언어', '문학', '역사');

INSERT INTO category (kdc_code, name)
SELECT '000', '총류'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '000');

INSERT INTO category (kdc_code, name)
SELECT '100', '철학'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '100');

INSERT INTO category (kdc_code, name)
SELECT '200', '종교'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '200');

INSERT INTO category (kdc_code, name)
SELECT '300', '사회과학'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '300');

INSERT INTO category (kdc_code, name)
SELECT '400', '자연과학'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '400');

INSERT INTO category (kdc_code, name)
SELECT '500', '기술과학'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '500');

INSERT INTO category (kdc_code, name)
SELECT '600', '예술'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '600');

INSERT INTO category (kdc_code, name)
SELECT '700', '언어'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '700');

INSERT INTO category (kdc_code, name)
SELECT '800', '문학'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '800');

INSERT INTO category (kdc_code, name)
SELECT '900', '역사'
WHERE NOT EXISTS (SELECT 1 FROM category WHERE kdc_code = '900');

UPDATE category
SET name = CASE kdc_code
    WHEN '000' THEN '총류'
    WHEN '100' THEN '철학'
    WHEN '200' THEN '종교'
    WHEN '300' THEN '사회과학'
    WHEN '400' THEN '자연과학'
    WHEN '500' THEN '기술과학'
    WHEN '600' THEN '예술'
    WHEN '700' THEN '언어'
    WHEN '800' THEN '문학'
    WHEN '900' THEN '역사'
END
WHERE kdc_code IN ('000', '100', '200', '300', '400', '500', '600', '700', '800', '900');

ALTER TABLE category
    MODIFY COLUMN kdc_code VARCHAR(3) NOT NULL;

SET @kdc_code_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'category'
      AND column_name = 'kdc_code'
      AND non_unique = 0
);

SET @add_kdc_code_unique_sql = IF(
    @kdc_code_unique_exists = 0,
    'ALTER TABLE category ADD CONSTRAINT uk_category_kdc_code UNIQUE (kdc_code)',
    'SELECT 1'
);

PREPARE add_kdc_code_unique_statement FROM @add_kdc_code_unique_sql;
EXECUTE add_kdc_code_unique_statement;
DEALLOCATE PREPARE add_kdc_code_unique_statement;
