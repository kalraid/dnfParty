-- DFO Party Management Application Initial Data

-- 서버 정보 (DFO API 서버 목록)
-- 실제로는 DFO API에서 동적으로 가져오지만, 테스트용으로 미리 설정
INSERT INTO adventures (adventure_name, server_id, character_count, total_fame, average_level, is_active, created_at, updated_at) 
VALUES 
    ('테스트모험단1', 'cain', 0, 0, 0.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('테스트모험단2', 'siroco', 0, 0, 0.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('테스트모험단3', 'prey', 0, 0, 0.0, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE 
    character_count = VALUES(character_count),
    total_fame = VALUES(total_fame),
    average_level = VALUES(average_level),
    updated_at = CURRENT_TIMESTAMP;

-- 테스트용 캐릭터 데이터 (실제로는 사용자가 검색해서 추가)
-- INSERT INTO characters (...) VALUES (...) ON DUPLICATE KEY UPDATE ...;
