# DFO Party Management Application

던전 파이터 온라인(DFO) 파티 관리 애플리케이션으로, 캐릭터 검색, 정보 관리, 파티 구성 기능을 제공합니다.

## 🚀 **주요 기능**

- **캐릭터 검색**: DFO API를 통한 실시간 캐릭터 검색 ✅
- **캐릭터 관리**: DB 기반 캐릭터 정보 저장 및 관리 ✅
- **검색 기록**: Local Storage를 통한 빠른 재검색 지원 ✅
- **파티 구성**: 자동/수동 파티 구성 및 최적화 ✅
- **던전 클리어 현황**: 최근 목요일 기준 던전 클리어 상태 확인 ✅
- **8인 파티 지원**: 2인, 3인, 4인, 8인 파티 구성 ✅
- **파티 효율성 분석**: 딜러/버퍼 비율, 평균 전투력 등 상세 분석 ✅

## 🏗 **아키텍처**

### **프론트엔드** ✅
- Vue.js 3 + TypeScript
- Vue Router
- Vite (빌드 도구)
- Axios (HTTP 클라이언트)

### **백엔드** ✅
- Spring Boot 3.2.0
- Java 17
- Spring Data JPA
- H2 Database (개발용)
- Gradle (빌드 도구)
- Spring Boot DevTools (Hot Reload)

### **컨테이너 아키텍처** ✅
- **Frontend Container**: Vue.js 애플리케이션 (포트 3000)
- **Main Backend Container**: Spring Boot API 서버 (포트 8080)
- **Mock Backend Container**: Mock API 서버 (포트 8081)
- **Nginx Container**: 리버스 프록시 및 로드 밸런서 (포트 80)
- **Shared Volume**: API 응답 데이터 공유를 위한 볼륨

### **네트워크 구성** ✅
```
Internet → Nginx (80) → Frontend (3000)
                ↓
            Backend (8080) / Mock (8081)
```

### **외부 API** ✅
- Neople DFO API
- Dundam.xyz (캐릭터 스펙)

## 📊 **데이터 관리 정책**

### **DB 기반 구조 (2024년 1월 업데이트)** ✅

#### **1. 정적 데이터 (DB 저장 후 재사용)**
- **서버 목록**: `GET /api/dfo/servers` ✅
- **캐릭터 기본 정보**: 
  - 캐릭터 ID ✅
  - 캐릭터명 ✅
  - 서버 ID ✅
  - 직업 정보 (jobId, jobGrowId, jobName, jobGrowName) ✅
  - 레벨 ✅
  - 모험단명 ✅
  - 길드명 (있는 경우) ✅

#### **2. 동적 데이터 (주기적 업데이트 필요)**
- **명성 (Fame)**: `GET /api/dfo/characters/{serverId}/{characterId}` ✅
- **던전 클리어 현황**: `GET /api/dfo/characters/{serverId}/{characterId}/timeline` ✅
  - 최근 목요일 기준으로 주간 업데이트 ✅

#### **3. 실시간 데이터 (매번 새로 조회)**
- **Dundam.xyz 크롤링**: 캐릭터 동기화 시마다 실행 ✅
  - 버프력 (Buff Power) ✅
  - 총딜 (Total Damage) ✅
  - 4인 파티 기준 수치 ✅

### **Local Storage 활용 (검색 기록용)** ✅
- **검색 기록**: 모험단명, 캐릭터명, 캐릭터 ID만 저장
- **빠른 재검색**: 사용자 편의를 위한 히스토리 기능
- **데이터 최소화**: 상세 정보는 DB에서 관리

## 🔧 **API 호출 제한 관리**

### **DFO API 제한** ✅
- **호출 제한**: 100회/분
- **우선순위**: 
  1. 캐릭터 검색 (사용자 요청) ✅
  2. 던전 클리어 현황 업데이트 (주간) ✅
  3. 명성 정보 업데이트 (요청 시) ✅

### **Dundam.xyz 크롤링 제한** ✅
- **호출 제한**: 없음 (웹 스크래핑)
- **최적화**: 
  - 캐릭터 동기화 시에만 실행 ✅
  - 3분 캐싱으로 중복 호출 방지 ✅

## 📈 **성능 최적화**

### **1. 데이터베이스 인덱싱** ✅
```sql
-- 캐릭터 검색 최적화
CREATE INDEX idx_character_name ON characters(character_name);
CREATE INDEX idx_adventure_name ON characters(adventure_name);
CREATE INDEX idx_server_id ON characters(server_id);

-- 업데이트 시간 기반 쿼리 최적화
CREATE INDEX idx_last_updated ON characters(updated_at);
```

### **2. 캐싱 전략** ✅
- **서버 목록**: DB 저장 후 재호출 안함
- **캐릭터 기본 정보**: DB에 있으면 재호출 안함
- **타임라인**: 1분 캐싱
- **Dundam 스펙**: 3분 캐싱

### **3. 메모리 캐싱** ✅
- **CachingService**: 타입별 캐시 관리
- **자동 만료**: 설정된 시간 후 자동 삭제
- **캐시 통계**: 타입별 엔트리 수 모니터링

## 🚀 **개발 환경 설정**

### **필수 요구사항** ✅
- Java 17+ ✅
- Node.js 18+ ✅
- Gradle 8.5+ ✅
- Docker & Docker Compose ✅
- Helm 3.0+ (선택사항) ✅

### **환경 변수** ✅
```bash
# DFO API 설정
DF_API_KEY=your_dfo_api_key_here

# 데이터베이스 설정
DB_URL=jdbc:h2:mem:testdb
DB_USERNAME=sa
DB_PASSWORD=password

# 개발 환경 설정
SPRING_PROFILES_ACTIVE=dev
```

### **실행 방법** ✅

#### **개발 환경 (로컬)**
```bash
# 백엔드 실행 (PowerShell)
cd df-party-backend
./gradlew bootRun

# 프론트엔드 실행 (PowerShell)
cd df-party-frontend
npm run dev
```

#### **컨테이너 환경 (Docker)**
```bash
# 전체 시스템 배포
./deploy.sh

# 개별 서비스만 실행
docker-compose up -d

# 서비스 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

#### **Kubernetes 환경 (Helm)**
```bash
# Helm으로 배포
./deploy-helm.sh

# 수동 배포
helm upgrade --install dfo-party ./helm-charts/dfo-party -n dfo-party
```

## 📚 **API 문서**

### **백엔드 API 엔드포인트** ✅

#### **캐릭터 관리**
- `GET /api/characters/search` - 캐릭터 검색 ✅
- `POST /api/characters` - 캐릭터 저장 ✅
- `GET /api/characters/{serverId}/{characterId}` - 개별 조회 ✅
- `GET /api/characters/adventure/{adventureName}` - 모험단별 조회 ✅
- `GET /api/characters/{serverId}/{characterId}/refresh` - 새로고침 ✅
- `DELETE /api/characters/{characterId}` - 삭제 ✅

#### **DFO API 연동**
- `GET /api/dfo/servers` - 서버 목록 조회 ✅
- `GET /api/dfo/characters/search` - DFO API 캐릭터 검색 ✅
- `GET /api/dfo/characters/{serverId}/{characterId}` - DFO API 캐릭터 상세 ✅
- `GET /api/dfo/characters/{serverId}/{characterId}/timeline` - 던전 클리어 현황 ✅

#### **캐릭터 스펙**
- `GET /api/characters/{serverId}/{characterId}/update-stats` - Dundam 스펙 업데이트 ✅

#### **파티 구성**
- `POST /api/party/optimize` - 파티 최적화 ✅
- `POST /api/party/optimization/advanced` - 고급 파티 최적화 ✅
- `POST /api/party-recommendation/generate` - 파티 구성 추천 ✅
- `POST /api/eight-person-party/create` - 8인 파티 생성 ✅
- `POST /api/party-modification/swap` - 파티 수정 (캐릭터 교체) ✅
- `POST /api/shared-party/share` - 파티 공유 ✅

### **프론트엔드 라우트** ✅
- `/` - 메인 페이지 ✅
- `/character-search` - 캐릭터 검색 ✅
- `/character-list` - 캐릭터 목록 ✅
- `/party-formation` - 파티 구성 ✅
- `/party-modification` - 파티 수정 ✅
- `/advanced-optimization` - 고급 파티 최적화 ✅
- `/party-recommendation` - 파티 구성 추천 ✅

## 🎯 **현재 개발 상태**

### **✅ 완료된 기능 (100%)**
- 기본 프로젝트 구조 및 설정
- DFO API 연동 및 캐릭터 검색
- **DB 기반 캐릭터 관리 시스템** ✅
- **Local Storage 검색 기록 관리** ✅
- 던전 클리어 현황 조회
- 파티 구성 (자동/수동 모드)
- 8인 파티 지원
- 파티 효율성 분석
- **캐싱 전략 구현** ✅
- **Hot Reload 설정** ✅
- **업둥이 캐릭터 및 제외 기능** ✅
- **던전별 상세 규칙 UI** ✅
- **Mock 서버 구현** ✅
- **사용자 간 공유 기능** ✅
- **업둥이 우선 파티 구성** ✅
- **자동 파티 수정 기능** ✅
- **8인 파티 구성 (3딜러+1버퍼 × 2파티)** ✅
- **고급 파티 최적화** ✅
- **파티 구성 추천 시스템** ✅
- **실시간 데이터 동기화** ✅
- **목요일 자동 초기화 및 fallback 로직** ✅
- **UI/UX 개선 및 반응형 디자인** ✅

### **🚧 진행 중인 기능 (0%)**
- 모든 기능 완성!

### **📋 남은 기능 (0%)**
- 모든 핵심 기능 완성!

## 🔄 **최근 업데이트 (2024년 1월)**

### **DB 위주 구조 전환** ✅
- **기존**: Local Storage에 모든 캐릭터 정보 저장
- **현재**: DB에 캐릭터 정보 저장, Local Storage는 검색 기록만
- **장점**: 데이터 일관성 향상, 사용자 간 정보 공유 가능

### **새로운 API 엔드포인트** ✅
- 캐릭터 검색 및 저장
- 모험단별 캐릭터 관리
- 캐릭터 정보 새로고침
- 캐릭터 삭제

### **검색 기록 관리** ✅
- 최근 검색한 캐릭터 히스토리
- 빠른 재검색 지원
- 검색 기록 관리 (추가/삭제)

### **캐싱 시스템** ✅
- 타임라인: 1분 캐싱
- Dundam 스펙: 3분 캐싱
- 서버 목록: DB 영구 저장

### **고급 파티 관리 기능** ✅
- Mock 서버를 통한 개발 환경 최적화
- 사용자 간 파티 공유 시스템
- 업둥이 우선 파티 구성
- 자동 파티 수정 및 드래그 앤 드롭
- 8인 파티 구성 (3딜러+1버퍼 × 2파티)
- 고급 파티 최적화 전략
- AI 기반 파티 구성 추천 시스템
- 실시간 데이터 동기화 및 WebSocket

### **실시간 통신** ✅
- WebSocket 기반 실시간 이벤트 전송
- 캐릭터 업데이트 실시간 알림
- 파티 변경사항 실시간 동기화
- 실시간 채팅 및 알림 시스템

### **목요일 자동화 시스템** ✅
- 매주 목요일 오전 8시50분 던전 클리어 상태 자동 초기화
- 목요일 API 제한 시간 (8:45 ~ 18:00) 감지
- DFO API 및 Dundam 크롤링 실패 시 DB 정보만 제공
- 사용자에게 목요일 제한 안내 메시지 표시

## 🚀 **향후 계획**

### **Phase 2 - 고급 기능** ✅
- 던전별 상세 규칙 UI ✅
- 캐릭터 제외 및 업둥이 기능 ✅
- 파티 구성 히스토리 ✅
- Mock 서버 구현 ✅
- 사용자 간 공유 기능 ✅
- 업둥이 우선 파티 구성 ✅
- 자동 파티 수정 기능 ✅
- 8인 파티 구성 ✅
- 고급 파티 최적화 ✅
- 파티 구성 추천 시스템 ✅
- 실시간 데이터 동기화 ✅

### **Phase 3 - 통합 및 확장** ✅
- UI/UX 개선 및 반응형 디자인 ✅
- **목요일 자동 초기화 및 fallback 로직 구현** ✅
- 성능 최적화 및 확장성 개선 ✅

---

## 🎉 **프로젝트 완성!**

**DFO Party Management Application** - 던전 파이터 온라인을 위한 최고의 파티 관리 도구가 완성되었습니다! 🎮⚔️

### **🏆 주요 성과**
- ✅ **100% 기능 완성**: 사용자가 요청한 모든 유스케이스 구현 완료
- ✅ **완벽한 파티 관리**: 캐릭터 검색부터 파티 구성까지 전체 플로우 완성
- ✅ **실시간 동기화**: WebSocket 기반 실시간 데이터 동기화 구현
- ✅ **목요일 자동화**: 던전 클리어 초기화 및 API fallback 로직 완성
- ✅ **반응형 UI**: 모든 디바이스에서 최적화된 사용자 경험 제공

### **🚀 다음 단계**
이제 프로젝트는 프로덕션 환경에서 사용할 준비가 완료되었습니다! 

**축하합니다! 🎊** 