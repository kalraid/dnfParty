-- 캐릭터 테이블에 누락된 컬럼들 추가

-- 캐릭터 이미지 컬럼 추가
ALTER TABLE characters 
ADD COLUMN IF NOT EXISTS character_image_url TEXT AFTER last_stats_update,
ADD COLUMN IF NOT EXISTS avatar_image_url TEXT AFTER character_image_url;

-- 던전별 업둥이 설정 컬럼 추가
ALTER TABLE characters 
ADD COLUMN IF NOT EXISTS is_favorite_nabel BOOLEAN DEFAULT FALSE AFTER avatar_image_url,
ADD COLUMN IF NOT EXISTS is_favorite_venus BOOLEAN DEFAULT FALSE AFTER is_favorite_nabel,
ADD COLUMN IF NOT EXISTS is_favorite_fog BOOLEAN DEFAULT FALSE AFTER is_favorite_venus,
ADD COLUMN IF NOT EXISTS is_favorite_twilight BOOLEAN DEFAULT FALSE AFTER is_favorite_fog;

-- 기존 is_favorite 데이터를 is_favorite_nabel로 마이그레이션
UPDATE characters SET is_favorite_nabel = is_favorite WHERE is_favorite = TRUE;

COMMIT;
