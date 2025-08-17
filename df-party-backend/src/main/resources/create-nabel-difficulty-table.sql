-- 나벨 난이도 선택 테이블 생성
CREATE TABLE IF NOT EXISTS nabel_difficulty_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id VARCHAR(255) NOT NULL,
    selected_difficulty ENUM('HARD', 'NORMAL', 'MATCHING') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스 추가
    INDEX idx_character_id (character_id),
    INDEX idx_difficulty (selected_difficulty)
);

-- 기존 데이터가 있다면 삭제 (테스트용)
-- DELETE FROM nabel_difficulty_selections;
