-- DFO Party Management Application Database Schema

-- 캐릭터 정보 테이블
CREATE TABLE IF NOT EXISTS characters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id VARCHAR(255) NOT NULL UNIQUE,
    character_name VARCHAR(255) NOT NULL,
    server_id VARCHAR(100) NOT NULL,
    job_id VARCHAR(100),
    job_grow_id VARCHAR(100),
    job_name VARCHAR(255),
    job_grow_name VARCHAR(255),
    level INTEGER,
    adventure_name VARCHAR(255),
    guild_name VARCHAR(255),
    
    -- 동적 정보 (주기적 업데이트 필요)
    fame BIGINT,
    dungeon_clear_nabel BOOLEAN DEFAULT FALSE,
    dungeon_clear_venus BOOLEAN DEFAULT FALSE,
    dungeon_clear_fog BOOLEAN DEFAULT FALSE,
    last_dungeon_check TIMESTAMP,
    
    -- 실시간 정보 (매번 새로 조회)
    buff_power BIGINT,
    total_damage BIGINT,
    dundam_source TEXT,
    last_stats_update TIMESTAMP,
    
    -- 메타 정보
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_excluded BOOLEAN DEFAULT FALSE,
    excluded_dungeons TEXT, -- JSON 형태로 저장 (예: ["navel", "venus"])
    is_favorite BOOLEAN DEFAULT FALSE
);

-- 모험단 정보 테이블
CREATE TABLE IF NOT EXISTS adventures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    adventure_name VARCHAR(255) NOT NULL UNIQUE,
    server_id VARCHAR(100) NOT NULL,
    character_count INTEGER DEFAULT 0,
    total_fame BIGINT DEFAULT 0,
    average_level DOUBLE DEFAULT 0.0,
    last_activity TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_character_name ON characters(character_name);
CREATE INDEX IF NOT EXISTS idx_adventure_name ON characters(adventure_name);
CREATE INDEX IF NOT EXISTS idx_server_id ON characters(server_id);
CREATE INDEX IF NOT EXISTS idx_last_updated ON characters(updated_at);
CREATE INDEX IF NOT EXISTS idx_last_dungeon_check ON characters(last_dungeon_check);
CREATE INDEX IF NOT EXISTS idx_last_stats_update ON characters(last_stats_update);
CREATE INDEX IF NOT EXISTS idx_fame ON characters(fame);
CREATE INDEX IF NOT EXISTS idx_is_excluded ON characters(is_excluded);

-- 모험단 인덱스
CREATE INDEX IF NOT EXISTS idx_adventure_server ON adventures(server_id);
CREATE INDEX IF NOT EXISTS idx_adventure_active ON adventures(is_active);
CREATE INDEX IF NOT EXISTS idx_adventure_last_activity ON adventures(last_activity);

-- 제약 조건
ALTER TABLE characters ADD CONSTRAINT IF NOT EXISTS fk_characters_server 
    FOREIGN KEY (server_id) REFERENCES adventures(server_id) ON DELETE CASCADE;

-- 초기 데이터 삽입 (테스트용)
INSERT INTO adventures (adventure_name, server_id, character_count, total_fame, average_level, is_active) 
VALUES ('테스트모험단', 'cain', 0, 0, 0.0, true)
ON DUPLICATE KEY UPDATE adventure_name = adventure_name;
