-- DFO Party Management Application Database Initialization Script
-- 이 스크립트는 기존 데이터를 백업하고 새로운 스키마를 적용합니다.

-- 1. 기존 테이블 백업 (선택사항)
-- CREATE TABLE characters_backup AS SELECT * FROM characters;
-- CREATE TABLE adventures_backup AS SELECT * FROM adventures;

-- 2. 기존 테이블 삭제
DROP TABLE IF EXISTS characters;
DROP TABLE IF EXISTS adventures;

-- 3. 모험단 테이블 생성
CREATE TABLE IF NOT EXISTS adventures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    adventure_name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. 캐릭터 테이블 생성 (모험단 참조)
CREATE TABLE IF NOT EXISTS characters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id VARCHAR(255) NOT NULL UNIQUE,
    character_name VARCHAR(255) NOT NULL,
    server_id VARCHAR(50) NOT NULL,
    adventure_id BIGINT,
    level INT,
    fame BIGINT,
    job_id VARCHAR(255),
    job_name VARCHAR(255),
    job_grow_id VARCHAR(255),
    job_grow_name VARCHAR(255),
    guild_name VARCHAR(255),
    character_image_url TEXT,
    avatar_image_url TEXT,
    
    -- 던전 클리어 상태
    dungeon_clear_nabel BOOLEAN DEFAULT FALSE,
    dungeon_clear_venus BOOLEAN DEFAULT FALSE,
    dungeon_clear_fog BOOLEAN DEFAULT FALSE,
    dungeon_clear_twilight BOOLEAN DEFAULT FALSE,
    dungeon_clear_azure BOOLEAN DEFAULT FALSE,
    dungeon_clear_storm BOOLEAN DEFAULT FALSE,
    dungeon_clear_temple BOOLEAN DEFAULT FALSE,
    dungeon_clear_nightmare BOOLEAN DEFAULT FALSE,
    
    -- 던전별 즐겨찾기
    is_favorite_nabel BOOLEAN DEFAULT FALSE,
    is_favorite_venus BOOLEAN DEFAULT FALSE,
    is_favorite_fog BOOLEAN DEFAULT FALSE,
    is_favorite_twilight BOOLEAN DEFAULT FALSE,
    
    -- 스탯 정보
    buff_power BIGINT,
    total_damage BIGINT,
    buff_power_2p BIGINT,
    buff_power_3p BIGINT,
    buff_power_4p BIGINT,
    total_damage_2p BIGINT,
    total_damage_3p BIGINT,
    total_damage_4p BIGINT,
    
    -- 수동 입력 스탯
    manual_buff_power BIGINT,
    manual_total_damage BIGINT,
    manual_buff_power_2p BIGINT,
    manual_buff_power_3p BIGINT,
    manual_buff_power_4p BIGINT,
    manual_total_damage_2p BIGINT,
    manual_total_damage_3p BIGINT,
    manual_total_damage_4p BIGINT,
    manual_updated_at TIMESTAMP,
    manual_updated_by VARCHAR(255),
    
    -- 기타 정보
    dundam_source VARCHAR(50),
    excluded_dungeons TEXT,
    is_excluded BOOLEAN DEFAULT FALSE,
    last_dungeon_check TIMESTAMP,
    last_stats_update TIMESTAMP,
    
    -- 타임스탬프
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 외래키 제약
    FOREIGN KEY (adventure_id) REFERENCES adventures(id) ON DELETE SET NULL,
    
    -- 인덱스
    INDEX idx_character_id (character_id),
    INDEX idx_adventure_id (adventure_id),
    INDEX idx_server_id (server_id),
    INDEX idx_character_name (character_name),
    INDEX idx_adventure_name (adventure_id)
);

-- 5. 초기 모험단 데이터 삽입
INSERT INTO adventures (adventure_name) VALUES 
('앙갚음사적단'),
('기타모험단1'),
('기타모험단2')
ON DUPLICATE KEY UPDATE adventure_name = VALUES(adventure_name);

-- 6. 성공 메시지 출력
SELECT 'Database schema updated successfully!' as status;
SELECT COUNT(*) as adventure_count FROM adventures;
SELECT COUNT(*) as character_count FROM characters;
