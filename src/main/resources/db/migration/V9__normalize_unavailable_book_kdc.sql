-- KDC 코드와 분류명 중 하나라도 신뢰할 수 없으면 미확보 상태인 null 쌍으로 통일한다.
UPDATE book
SET kdc_code = NULL,
    kdc_name = NULL
WHERE kdc_name = '미분류'
   OR kdc_code IS NULL
   OR CHAR_LENGTH(TRIM(kdc_code)) <> 3
   OR kdc_name IS NULL
   OR TRIM(kdc_name) = '';
