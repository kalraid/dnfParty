-- 매칭 나벨 필드 추가 마이그레이션
-- 실행 전 백업 권장

-- characters 테이블에 is_matching_nabel_eligible 필드 추가
ALTER TABLE characters 
ADD COLUMN is_matching_nabel_eligible BOOLEAN DEFAULT FALSE 
AFTER is_normal_nabel_eligible;

-- 기존 데이터에 대해 매칭 나벨 대상자 여부 업데이트
-- 명성 6만 이상인 캐릭터를 매칭 대상자로 설정
UPDATE characters 
SET is_matching_nabel_eligible = TRUE 
WHERE fame >= 60000;

-- 인덱스 추가 (선택사항)
-- CREATE INDEX idx_matching_nabel_eligible ON characters(is_matching_nabel_eligible);

-- 변경사항 확인
SELECT 
    character_name,
    fame,
    is_hard_nabel_eligible,
    is_normal_nabel_eligible,
    is_matching_nabel_eligible
FROM characters 
WHERE fame >= 60000 
LIMIT 10;
