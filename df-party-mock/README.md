# DFO Party Mock Server

DFO Party Management Application을 위한 Mock API 서버입니다.

## 🎯 **목적**

- 개발 및 테스트 환경에서 실제 API 호출 없이 기능 테스트
- API 응답 데이터 자동 저장 및 재사용
- Mock 모드와 실제 API 모드 간 전환 지원

## 🚀 **기능**

### **Mock API 제공**
- **DFO API Mock**: 서버 목록, 캐릭터 검색, 상세 정보, 타임라인
- **Dundam API Mock**: 캐릭터 정보, 스펙 데이터
- **Mock 관리 API**: 응답 조회, 비활성화, 상태 확인

### **자동 데이터 저장**
- API 호출 시마다 요청/응답 데이터 자동 저장
- Mock 데이터로 동일한 응답 재현
- 접근 횟수 및 마지막 접근 시간 추적

## 🛠 **기술 스택**

- **Spring Boot 3.2.0** + **Java 17**
- **H2 Database** (Mock 데이터 저장)
- **Spring Data JPA**
- **Lombok**
- **Jackson** (JSON 처리)

## 📁 **프로젝트 구조**

```
src/main/java/com/dfpartymock/
├── config/
│   └── MockConfig.java              # Mock 설정
├── controller/
│   ├── MockDfoApiController.java    # DFO API Mock
│   ├── MockDundamApiController.java # Dundam API Mock
│   └── MockManagementController.java # Mock 관리
├── model/
│   └── MockApiResponse.java         # Mock 응답 모델
├── service/
│   └── MockApiService.java          # Mock API 서비스
└── DfPartyMockApplication.java      # 메인 애플리케이션
```

## ⚙️ **설정**

### **application.yml 주요 설정**

```yaml
server:
  port: 8081  # 메인 백엔드와 다른 포트

spring:
  mock:
    enabled: true           # Mock 모드 활성화
    data-path: ./mock-data # Mock 데이터 저장 경로
    auto-save: true        # API 호출 시마다 자동 저장
    response-delay: 100    # 응답 지연 시간 (ms)

mock-api:
  dfo:
    enabled: true          # DFO Mock API 활성화
    auto-save: true        # 자동 저장
  dundam:
    enabled: true          # Dundam Mock API 활성화
    auto-save: true        # 자동 저장
```

## 🚀 **실행 방법**

### **1. 프로젝트 빌드**
```bash
./gradlew build
```

### **2. Mock 서버 실행**
```bash
./gradlew bootRun
```

### **3. 접속 확인**
- Mock 서버: http://localhost:8081
- H2 Console: http://localhost:8081/h2-console

## 📡 **API 엔드포인트**

### **DFO Mock API**
- `GET /mock/dfo/servers` - 서버 목록
- `GET /mock/dfo/characters/search` - 캐릭터 검색
- `GET /mock/dfo/characters/{serverId}/{characterId}` - 캐릭터 상세
- `GET /mock/dfo/characters/{serverId}/{characterId}/timeline` - 타임라인

### **Dundam Mock API**
- `GET /mock/dundam/character` - 캐릭터 정보
- `GET /mock/dundam/character/stats` - 캐릭터 스펙

### **Mock 관리 API**
- `GET /mock/management/responses` - 모든 Mock 응답 조회
- `GET /mock/management/status` - Mock 서버 상태
- `PATCH /mock/management/responses/{id}/deactivate` - 응답 비활성화
- `POST /mock/management/reset` - Mock 데이터 초기화

## 🔄 **사용 방법**

### **1. Mock 모드 활성화**
```yaml
spring:
  mock:
    enabled: true
```

### **2. API 호출**
```bash
# DFO API Mock 호출
curl "http://localhost:8081/mock/dfo/servers"

# Dundam API Mock 호출
curl "http://localhost:8081/mock/dundam/character?server=bakal&key=test"
```

### **3. Mock 데이터 확인**
```bash
# H2 Console 접속
# JDBC URL: jdbc:h2:file:./mock-data/mockdb
# Username: sa
# Password: password
```

## 📊 **Mock 데이터 구조**

### **MockApiResponse 테이블**
- `api_type`: API 유형 (DFO_SERVERS, DFO_CHARACTER_SEARCH 등)
- `endpoint`: API 엔드포인트
- `request_method`: HTTP 메서드
- `request_params`: 요청 파라미터 (JSON)
- `response_body`: 응답 데이터 (JSON)
- `created_at`: 생성 시간
- `last_accessed`: 마지막 접근 시간
- `access_count`: 접근 횟수
- `is_active`: 활성 상태

## 🔧 **개발 가이드**

### **새로운 Mock API 추가**
1. `MockApiResponse.ApiType`에 새로운 타입 추가
2. 컨트롤러에 새로운 엔드포인트 구현
3. Mock 응답 데이터 생성 메서드 구현
4. `saveMockResponse` 호출로 자동 저장

### **Mock 데이터 커스터마이징**
- `createMock*Response` 메서드에서 반환 데이터 수정
- 실제 API 응답과 동일한 구조로 데이터 구성

## ⚠️ **주의사항**

- Mock 서버는 **개발/테스트 환경 전용**
- 실제 운영 환경에서는 사용하지 않음
- Mock 데이터는 H2 파일 데이터베이스에 저장
- API 호출 시마다 자동으로 데이터 저장 (설정에 따라)

## 🚀 **향후 계획**

- [ ] Mock 데이터 백업/복원 기능
- [ ] Mock 응답 편집 UI
- [ ] 실제 API 응답과 Mock 응답 비교 기능
- [ ] Mock 데이터 통계 및 분석
- [ ] Mock 응답 템플릿 시스템
