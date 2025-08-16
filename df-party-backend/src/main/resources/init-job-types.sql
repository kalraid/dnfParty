-- 직업 타입 정보 초기화 스크립트
-- DFMAX 직업 분석 정보를 바탕으로 버퍼/딜러 구분

-- 직업 타입 테이블 생성
CREATE TABLE IF NOT EXISTS job_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL UNIQUE,
    job_grow_name VARCHAR(100),
    is_buffer BOOLEAN NOT NULL DEFAULT FALSE,
    is_dealer BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 기존 데이터 삭제 (재실행 시)
DELETE FROM job_types;

-- 버퍼 직업들 (DFMAX 기준)
INSERT INTO job_types (job_name, job_grow_name, is_buffer, is_dealer) VALUES
-- 아처 계열
('뮤즈', '眞 뮤즈', TRUE, FALSE),
('트래블러', '眞 트래블러', TRUE, FALSE),
('헌터', '眞 헌터', TRUE, FALSE),
('비질란테', '眞 비질란테', TRUE, FALSE),
('키메라', '眞 키메라', TRUE, FALSE),

-- 프리스트 계열
('크루세이더', '眞 크루세이더(남)', TRUE, FALSE),
('크루세이더', '眞 크루세이더(여)', TRUE, FALSE),
('무녀', '眞 무녀', TRUE, FALSE),
('미스트리스', '眞 미스트리스', TRUE, FALSE),

-- 마법사 계열
('엘레멘탈마스터', '眞 엘레멘탈마스터', TRUE, FALSE),
('소환사', '眞 소환사', TRUE, FALSE),
('마도학자', '眞 마도학자', TRUE, FALSE),
('인챈트리스', '眞 인챈트리스', TRUE, FALSE),

-- 기타 버퍼
('패러메딕', '眞 패러메딕', TRUE, FALSE);

-- 딜러 직업들 (DFMAX 기준)
INSERT INTO job_types (job_name, job_grow_name, is_buffer, is_dealer) VALUES
-- 귀검사 계열
('웨펀마스터', '眞 웨펀마스터', FALSE, TRUE),
('소울브링어', '眞 소울브링어', FALSE, TRUE),
('버서커', '眞 버서커', FALSE, TRUE),
('아수라', '眞 아수라', FALSE, TRUE),
('검귀', '眞 검귀', FALSE, TRUE),
('소드마스터', '眞 소드마스터', FALSE, TRUE),
('다크템플러', '眞 다크템플러', FALSE, TRUE),
('데몬슬레이어', '眞 데몬슬레이어', FALSE, TRUE),
('베가본드', '眞 베가본드', FALSE, TRUE),
('블레이드', '眞 블레이드', FALSE, TRUE),

-- 격투가 계열
('넨마스터', '眞 넨마스터(남)', FALSE, TRUE),
('넨마스터', '眞 넨마스터(여)', FALSE, TRUE),
('스트라이커', '眞 스트라이커(남)', FALSE, TRUE),
('스트라이커', '眞 스트라이커(여)', FALSE, TRUE),
('스트리트파이터', '眞 스트리트파이터(남)', FALSE, TRUE),
('스트리트파이터', '眞 스트리트파이터(여)', FALSE, TRUE),
('그래플러', '眞 그래플러(남)', FALSE, TRUE),
('그래플러', '眞 그래플러(여)', FALSE, TRUE),

-- 거너 계열
('레인저', '眞 레인저(남)', FALSE, TRUE),
('레인저', '眞 레인저(여)', FALSE, TRUE),
('런처', '眞 런처(남)', FALSE, TRUE),
('런처', '眞 런처(여)', FALSE, TRUE),
('메카닉', '眞 메카닉(남)', FALSE, TRUE),
('메카닉', '眞 메카닉(여)', FALSE, TRUE),
('스핏파이어', '眞 스핏파이어(남)', FALSE, TRUE),
('스핏파이어', '眞 스핏파이어(여)', FALSE, TRUE),
('어썰트', '眞 어썰트', FALSE, TRUE),

-- 마법사 계열
('엘레멘탈 바머', '眞 엘레멘탈 바머', FALSE, TRUE),
('빙결사', '眞 빙결사', FALSE, TRUE),
('블러드 메이지', '眞 블러드 메이지', FALSE, TRUE),
('스위프트 마스터', '眞 스위프트 마스터', FALSE, TRUE),
('디멘션워커', '眞 디멘션워커', FALSE, TRUE),
('배틀메이지', '眞 배틀메이지', FALSE, TRUE),

-- 프리스트 계열
('인파이터', '眞 인파이터', FALSE, TRUE),
('퇴마사', '眞 퇴마사', FALSE, TRUE),
('어벤저', '眞 어벤저', FALSE, TRUE),
('이단심판관', '眞 이단심판관', FALSE, TRUE),

-- 도적 계열
('로그', '眞 로그', FALSE, TRUE),
('사령술사', '眞 사령술사', FALSE, TRUE),
('쿠노이치', '眞 쿠노이치', FALSE, TRUE),
('섀도우댄서', '眞 섀도우댄서', FALSE, TRUE),

-- 나이트 계열
('엘븐나이트', '眞 엘븐나이트', FALSE, TRUE),
('카오스', '眞 카오스', FALSE, TRUE),
('팔라딘', '眞 팔라딘', FALSE, TRUE),
('드래곤나이트', '眞 드래곤나이트', FALSE, TRUE),

-- 마창사 계열
('뱅가드', '眞 뱅가드', FALSE, TRUE),
('듀얼리스트', '眞 듀얼리스트', FALSE, TRUE),
('드래고니안 랜서', '眞 드래고니안 랜서', FALSE, TRUE),
('다크 랜서', '眞 다크 랜서', FALSE, TRUE),

-- 총검사 계열
('히트맨', '眞 히트맨', FALSE, TRUE),
('요원', '眞 요원', FALSE, TRUE),
('트러블 슈터', '眞 트러블 슈터', FALSE, TRUE),
('스페셜리스트', '眞 스페셜리스트', FALSE, TRUE),

-- 크리에이터 계열
('크리에이터', '眞 크리에이터', FALSE, TRUE),

-- 다크나이트 계열
('다크나이트', '眞 다크나이트', FALSE, TRUE);

-- 직업 타입 조회를 위한 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_job_types_job_name ON job_types(job_name);
CREATE INDEX IF NOT EXISTS idx_job_types_job_grow_name ON job_types(job_grow_name);
CREATE INDEX IF NOT EXISTS idx_job_types_is_buffer ON job_types(is_buffer);
CREATE INDEX IF NOT EXISTS idx_job_types_is_dealer ON job_types(is_dealer);
