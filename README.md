# DFO Party Management Application

던전앤파이터 파티 관리 시스템으로, 캐릭터 정보 동기화, 던전 클리어 현황 관리, 파티 최적화 등의 기능을 제공합니다.

## 🚀 주요 기능

### 🎯 캐릭터 관리
- **DFO API 연동**: Neople 공식 API를 통한 캐릭터 정보 조회
- **던담 크롤링**: Playwright를 이용한 실시간 스탯 정보 수집
- **캐릭터 검색**: 서버별, 직업별, 이름별 캐릭터 검색
- **스탯 관리**: 버프력, 전투력, 명성 등 상세 스탯 정보
- **수동 스탯 입력**: API 데이터가 없는 경우 수동으로 스탯 입력 가능
- **캐릭터 동기화**: 2분 간격 제한으로 안전한 데이터 동기화

### 🏰 던전 클리어 현황
- **던전별 클리어 상태**: 나벨, 베누스, 안개신, 황혼전 클리어 현황
- **자동 초기화**: 매주 목요일 오전 8시 자동 초기화
- **던전별 제한**: 나벨 하드(4명), 황혼전(8명), 안개신(20명), 베누스(제한 없음)
- **나벨 난이도 분리**: 일반/하드 모드별 클리어 현황 표시
- **나벨 난이도 선택**: 매칭, 일반, 하드 모드별 적격성 체크

### 👥 파티 구성 및 최적화
- **8인 파티 구성**: 자동 파티 매칭 및 최적화
- **파티 수정**: 기존 파티 구성 변경 및 개선
- **고급 파티 빌더**: 상세한 조건 설정을 통한 파티 구성
- **파티 추천**: 던전별 최적 파티 구성 추천
- **업둥이 우선 파티**: 업둥이 캐릭터를 우선으로 하는 파티 구성
- **공유 파티**: 파티 구성 공유 및 코드 기반 조회

### 🔄 실시간 동기화
- **자동 동기화**: 설정된 간격으로 캐릭터 정보 자동 업데이트
- **SSE(Server-Sent Events)**: 실시간 알림 및 업데이트
- **동기화 제한**: 2분 간격 제한으로 API 과부하 방지
- **실시간 이벤트**: 모험단별 실시간 업데이트 및 알림

### 📊 데이터 관리
- **MariaDB 연동**: 영구 데이터 저장 및 관리
- **JobType 관리**: 직업별 버퍼/딜러 구분 자동화
- **모험단 정보**: 모험단별 캐릭터 그룹 관리
- **데이터 마이그레이션**: Local Storage에서 DB로 데이터 이전
- **서버 초기화**: 서버 정보 자동 초기화 및 관리

### 🧠 시스템 모니터링
- **메모리 모니터링**: JVM 메모리 사용량 실시간 모니터링
- **성능 최적화**: 메모리 누수 방지 및 GC 최적화
- **시스템 상태**: 실시간 시스템 상태 확인 및 관리

### 🚫 API 제한 관리
- **목요일 제한**: DFO API 점검 시간(8:00 ~ 10:00) 제한 관리
- **동기화 제한**: 캐릭터별 동기화 간격 제한
- **연결 제한**: SSE 연결 수 제한 및 관리

## 🛠️ 기술 스택

### Backend
- **Java**: 17 (OpenJDK)
- **Spring Boot**: 3.2.1
- **Spring Data JPA**: 데이터베이스 ORM
- **Gradle**: 8.4 (Wrapper 사용)
- **MariaDB**: 10.11 (MySQL 호환)
- **Playwright**: 웹 크롤링 및 자동화
- **Lombok**: 보일러플레이트 코드 감소

### Frontend
- **Vue.js**: 3.x (Composition API)
- **TypeScript**: 타입 안전성
- **Vite**: 7.1.2 (빌드 도구)
- **Pinia**: 상태 관리
- **Vue Router**: SPA 라우팅
- **CSS3**: 반응형 디자인 및 애니메이션

### Infrastructure
- **Kubernetes**: 1.24+
- **Helm**: 3.12+ (패키지 관리)
- **Nginx**: 리버스 프록시 및 로드 밸런서
- **Docker**: 컨테이너화

## 🏗️ 아키텍처

### 시스템 구성
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   Database      │
│   (Vue.js)      │◄──►│ (Spring Boot)   │◄──►│   (MariaDB)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Nginx         │    │   Playwright    │    │   JobType       │
│ (Load Balancer) │    │  (Crawling)     │    │  (Job Info)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Helm 차트 구조
- **Infrastructure**: MariaDB, Nginx (안정적 구성요소)
- **Application**: Backend, Frontend (빈번한 업데이트)

## 📁 프로젝트 구조

```
game/
├── df-party-backend/                    # Spring Boot 백엔드
│   ├── src/main/java/com/dfparty/backend/
│   │   ├── controller/                  # REST API 컨트롤러
│   │   │   ├── CharacterController.java     # 캐릭터 관리 API
│   │   │   ├── DungeonClearController.java  # 던전 클리어 API
│   │   │   ├── PartyController.java         # 파티 관리 API
│   │   │   ├── DundamSyncController.java    # 던담 동기화 API
│   │   │   ├── DfoApiController.java        # DFO API 연동
│   │   │   ├── SseController.java           # SSE 실시간 통신
│   │   │   ├── MemoryController.java        # 메모리 모니터링
│   │   │   ├── JobTypeController.java       # 직업 타입 관리
│   │   │   └── ...                         # 기타 컨트롤러
│   │   ├── service/                     # 비즈니스 로직 서비스
│   │   │   ├── CharacterService.java        # 캐릭터 비즈니스 로직
│   │   │   ├── PartyOptimizationService.java # 파티 최적화
│   │   │   ├── PlaywrightCrawlingService.java # 웹 크롤링
│   │   │   ├── DungeonClearResetService.java # 던전 클리어 초기화
│   │   │   ├── ThursdayFallbackService.java  # 목요일 API 제한
│   │   │   ├── MemoryMonitoringService.java  # 메모리 모니터링
│   │   │   └── ...                         # 기타 서비스
│   │   ├── entity/                      # JPA 엔티티
│   │   │   ├── Character.java               # 캐릭터 엔티티
│   │   │   ├── Adventure.java              # 모험단 엔티티
│   │   │   ├── JobType.java                # 직업 타입 엔티티
│   │   │   ├── Server.java                 # 서버 엔티티
│   │   │   └── NabelDifficultySelection.java # 나벨 난이도 선택
│   │   ├── repository/                  # 데이터 접근 계층
│   │   ├── dto/                         # 데이터 전송 객체
│   │   ├── utils/                       # 유틸리티 클래스
│   │   │   └── CharacterUtils.java         # 캐릭터 유틸리티
│   │   └── config/                      # 설정 클래스
│   └── src/main/resources/              # 설정 파일 및 SQL
├── df-party-frontend/                   # Vue.js 프론트엔드
│   ├── src/
│   │   ├── views/                       # 페이지 컴포넌트
│   │   │   ├── DungeonStatus.vue          # 던전 클리어 현황
│   │   │   ├── PartyFormation.vue         # 파티 구성
│   │   │   ├── CharacterSearch.vue        # 캐릭터 검색
│   │   │   ├── AdvancedPartyOptimization.vue # 고급 파티 최적화
│   │   │   └── ...                       # 기타 페이지
│   │   ├── components/                  # 재사용 컴포넌트
│   │   │   ├── AdvancedPartyBuilder.vue   # 고급 파티 빌더
│   │   │   ├── ThemeToggle.vue            # 테마 전환
│   │   │   ├── RealtimeNotification.vue   # 실시간 알림
│   │   │   └── ...                       # 기타 컴포넌트
│   │   ├── stores/                      # Pinia 상태 관리
│   │   ├── services/                    # API 서비스
│   │   ├── utils/                       # 유틸리티 함수
│   │   │   └── characterUtils.ts           # 캐릭터 유틸리티
│   │   └── types/                       # TypeScript 타입 정의
├── helm-charts/                         # 애플리케이션 Helm 차트
├── helm-charts-infrastructure/          # 인프라 Helm 차트
└── deploy-*.ps1                         # 배포 스크립트
```

## 🚀 배포 방법

### 1. Infrastructure 배포 (최초 1회)
```powershell
.\deploy-infrastructure.ps1 -Force
```

### 2. Application 배포
```powershell
.\deploy-application.ps1
```

### 3. 전체 시스템 배포
```powershell
# 1. Infrastructure 배포
.\deploy-infrastructure.ps1 -Force

# 2. Application 배포
.\deploy-application.ps1
```

## 💻 개발 환경 설정

### Backend 개발
```bash
cd df-party-backend
./gradlew bootRun
```

### Frontend 개발
```bash
cd df-party-frontend
npm install
npm run dev
```

### 환경 변수 설정
```bash
# config.env 파일 생성
cp config.env.example config.env

# DFO API 키 설정
DF_API_KEY=your_api_key_here
```

## 🔧 주요 API 엔드포인트

### 캐릭터 관리
- `GET /api/characters/{serverId}/{characterName}/complete` - 통합 캐릭터 정보
- `POST /api/characters/{characterId}/manual-stats` - 수동 스탯 업데이트
- `GET /api/characters/search` - 캐릭터 검색
- `POST /api/characters/{characterId}/twilight-eligibility` - 황혼전 적격성 업데이트

### 던전 클리어
- `GET /api/dungeon-clear/{serverId}/{characterId}` - 던전 클리어 상태
- `POST /api/dungeon-clear/{serverId}/bulk` - 대량 던전 클리어 상태
- `POST /api/dungeon-clear/reset` - 수동 초기화

### 파티 관리
- `POST /api/eight-person-party/create` - 8인 파티 생성
- `POST /api/party-optimization/updoongi-priority` - 업둥이 우선 파티
- `POST /api/party-recommendation` - 파티 추천
- `POST /api/advanced-party-optimization` - 고급 파티 최적화
- `POST /api/party-modification` - 파티 수정

### 동기화
- `POST /api/dundam-sync/character/{serverId}/{characterId}` - 던담 동기화
- `POST /api/realtime/adventure/{adventureName}/refresh` - 실시간 업데이트
- `GET /api/sse/events` - SSE 이벤트 스트림

### 시스템 관리
- `GET /api/memory/status` - 메모리 상태 확인
- `POST /api/memory/gc` - 수동 GC 실행
- `GET /api/job-types` - 직업 타입 조회
- `POST /api/job-types/sync-from-dfo` - DFO API에서 직업 정보 동기화

## 📊 데이터베이스 스키마

### 주요 테이블
- **characters**: 캐릭터 기본 정보 및 스탯
- **adventures**: 모험단 정보
- **job_types**: 직업별 버퍼/딜러 구분
- **servers**: 서버 정보
- **shared_parties**: 공유 파티 정보
- **nabel_difficulty_selections**: 나벨 난이도 선택 정보

## 🔄 스케줄링

### 자동 작업
- **던전 클리어 초기화**: 매주 목요일 오전 8시
- **캐릭터 동기화**: 1시간마다 (설정 가능)
- **메모리 모니터링**: 5분마다
- **던담 동기화**: 설정된 간격 (기본값: 1분)

## 🚨 제한사항

### API 제한
- **목요일 제한**: 오전 8시 ~ 10시 (KST, 한국 표준시) - DFO API 점검 시간
- **동기화 제한**: 캐릭터당 2분 간격
- **연결 제한**: 클라이언트당 최대 3개 SSE 연결

### 던전 제한
- **나벨 일반**: 제한 없음
- **나벨 하드**: 최대 4명
- **황혼전**: 최대 8명
- **안개신**: 최대 20명
- **베누스**: 제한 없음

### 적격성 기준
- **나벨 하드**: 명성 47,684, 버퍼 버프력 5,000,000, 딜러 총딜 10,000,000,000
- **나벨 일반**: 명성 47,684, 버퍼 버프력 4,000,000, 딜러 총딜 10,000,000,000
- **황혼전**: 명성 72,688, 버퍼 버프력 5,200,000, 딜러 총딜 10,000,000,000
- **안개신**: 명성 30,135
- **베누스**: 명성 41,929
## 🐛 문제 해결

### 일반적인 문제
1. **Backend Health Check 실패**: `/actuator/health` 엔드포인트 확인
2. **데이터베이스 연결**: Infrastructure 상태 및 MariaDB Pod 확인
3. **Gradle 빌드**: `./gradlew clean --no-daemon` 실행
4. **Docker 권한**: `chmod +x gradlew` 실행

### 로그 확인
```powershell
# Pod 상태 확인
kubectl get pods -n dfo

# 애플리케이션 로그
kubectl logs -f <pod-name> -n dfo

# Helm 상태 확인
helm status df-party-application -n dfo
```

## 📝 라이선스

이 프로젝트는 교육 및 개인 사용 목적으로 개발되었습니다.

## 🤝 기여

버그 리포트나 기능 제안은 이슈를 통해 제출해 주세요.

---

**마지막 업데이트**: 2024년 12월 19일  
**버전**: 1.0.0  
**개발팀**: DFO Party Management Team 