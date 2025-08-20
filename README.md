# DFO Party Management Application

DFO Party Management Application을 위한 Kubernetes 기반 배포 시스템입니다.

## 🛠️ 개발 환경

### Backend
- **Java**: 17 (OpenJDK)
- **Spring Boot**: 3.2.1
- **Gradle**: 8.4 (Wrapper 사용)
- **Build Tool**: Gradle Wrapper (`./gradlew`)

### Frontend
- **Node.js**: 18+ (LTS 권장)
- **Vue.js**: 3.x (Composition API)
- **Build Tool**: Vite 7.1.2
- **Package Manager**: npm

### Infrastructure
- **Kubernetes**: 1.24+
- **Helm**: 3.12+
- **Database**: MariaDB 10.11
- **Proxy**: Nginx 1.25

## 아키텍처

이 프로젝트는 **Infrastructure**와 **Application** 두 개의 독립적인 Helm 차트로 구성되어 있습니다:

### 🏗️ Infrastructure (안정적 구성요소)
- **MariaDB**: 데이터베이스 서버
- **Nginx**: 리버스 프록시 및 로드 밸런서

### 🚀 Application (빈번한 업데이트)
- **Backend**: Spring Boot API 서버
- **Frontend**: Vue.js 웹 애플리케이션

## 배포 방법

### 1. Infrastructure 배포 (최초 1회 또는 설정 변경 시)

```powershell
# PowerShell
.\deploy-infrastructure.ps1 -Force

# 또는 수동으로
helm upgrade --install df-party-infrastructure ./helm-charts-infrastructure -n dfo --create-namespace
```

### 2. Application 배포 (코드 변경 시마다)

```powershell
# PowerShell
.\deploy-application.ps1

# 또는 수동으로
helm upgrade --install df-party-application ./helm-charts -n dfo --create-namespace
```

### 3. 전체 시스템 배포

```powershell
# 1. Infrastructure 배포
.\deploy-infrastructure.ps1 -Force

# 2. Application 배포
.\deploy-application.ps1
```

## 🚀 개발 환경 설정

### Backend 개발 환경

```bash
# Gradle Wrapper 권한 설정 (Linux/Mac)
chmod +x gradlew

# 의존성 다운로드
./gradlew dependencies

# 애플리케이션 실행
./gradlew bootRun

# 빌드
./gradlew clean build -x test
```

### Frontend 개발 환경

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 빌드
npm run build
```

### Docker 개발 환경

```bash
# Backend 이미지 빌드
docker build -t df-party-backend:dev ./df-party-backend

# Frontend 이미지 빌드
docker build -t df-party-frontend:dev ./df-party-frontend
```

## 개발 워크플로우

### 일반적인 개발 과정

1. **코드 변경**: Backend 또는 Frontend 코드 수정
2. **이미지 빌드**: `.\deploy.ps1` 실행
3. **애플리케이션 재배포**: `.\deploy-application.ps1` 실행
4. **Infrastructure는 유지**: MariaDB와 Nginx는 그대로 유지

### Infrastructure 변경 시

1. **설정 변경**: Helm 차트 설정 수정
2. **Infrastructure 재배포**: `.\deploy-infrastructure.ps1 -Force` 실행
3. **주의**: MariaDB 데이터는 PVC를 통해 보존되지만, `-Force` 플래그 사용 시 삭제될 수 있음

## 파일 구조

```
game/
├── helm-charts-infrastructure/          # Infrastructure Helm 차트
│   ├── Chart.yaml                      # MariaDB + Nginx 의존성
│   ├── values.yaml                     # Infrastructure 설정
│   └── README.md                       # Infrastructure 문서
├── helm-charts/                        # Application Helm 차트
│   ├── Chart.yaml                      # Backend + Frontend 의존성
│   ├── values.yaml                     # Application 설정
│   ├── backend/                        # Backend 서브차트
│   ├── frontend/                       # Frontend 서브차트
│   └── README.md                       # Application 문서
├── deploy-infrastructure.ps1           # Infrastructure 배포 스크립트
├── deploy-application.ps1              # Application 배포 스크립트
├── deploy.ps1                          # Docker 이미지 빌드/푸시 스크립트
└── README.md                           # 이 파일
```

## 장점

### 🎯 분리된 관리
- **Infrastructure**: 자주 변경되지 않는 구성요소
- **Application**: 빈번한 업데이트가 필요한 구성요소

### 🚀 효율적인 배포
- Infrastructure는 한 번 배포 후 유지
- Application만 필요할 때마다 재배포
- 개발 시간 단축

### 💾 데이터 보존
- MariaDB 데이터는 PVC를 통해 보존
- Infrastructure 재배포 시에도 데이터 유지

### 🔄 유연한 업데이트
- Backend/Frontend만 독립적으로 업데이트 가능
- 전체 시스템 중단 없이 부분 업데이트

## 주의사항

### Infrastructure 배포 시
- `-Force` 플래그 사용 시 MariaDB 데이터가 삭제될 수 있음
- Nginx 설정 변경 시 ConfigMap 수정 필요

### Application 배포 시
- Infrastructure가 먼저 배포되어 있어야 함
- `pullPolicy: Always`로 최신 이미지 자동 배포

## 문제 해결

### 일반적인 문제들

1. **Backend Health Check 실패**
   - `/actuator/health` 엔드포인트 404 오류
   - 애플리케이션 로그 확인 필요

2. **데이터베이스 연결 문제**
   - Infrastructure 상태 확인
   - MariaDB Pod 상태 및 로그 확인

3. **이미지 업데이트 문제**
   - `pullPolicy: Always` 설정 확인
   - Docker 이미지 푸시 상태 확인

4. **Gradle 빌드 문제**
   - Lombok 어노테이션 처리 실패
   - Gradle Wrapper 사용 확인 (`./gradlew` 사용)
   - 의존성 캐시 클리어: `./gradlew clean --no-daemon`

5. **Docker 빌드 문제**
   - Gradle Wrapper 권한 확인: `chmod +x gradlew`
   - 시스템 Gradle 대신 프로젝트 Gradle Wrapper 사용

### 로그 확인

```powershell
# Pod 상태 확인
kubectl get pods -n dfo

# 서비스 상태 확인
kubectl get services -n dfo

# 애플리케이션 로그 확인
kubectl logs -f <pod-name> -n dfo

# Infrastructure 상태 확인
helm status df-party-infrastructure -n dfo
helm status df-party-application -n dfo
```

## 지원

문제가 발생하거나 질문이 있으시면 이슈를 생성해 주세요. 