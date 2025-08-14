# DFO Party Application Helm Chart

이 Helm 차트는 DFO Party 애플리케이션의 핵심 구성요소를 관리합니다.

## 구성 요소

- **Backend**: Spring Boot API 서버
- **Frontend**: Vue.js 웹 애플리케이션

## 특징

- **빈번한 업데이트**: 애플리케이션 코드 변경 시 자주 재배포
- **인프라 독립**: MariaDB와 Nginx는 별도로 관리
- **자동 이미지 풀**: `pullPolicy: Always`로 최신 이미지 자동 배포

## 사전 요구사항

Infrastructure가 먼저 배포되어야 합니다:

```powershell
.\deploy-infrastructure.ps1
```

## 배포 방법

### 1. 애플리케이션 배포

```powershell
# PowerShell
.\deploy-application.ps1

# 또는 수동으로
helm upgrade --install df-party-application ./helm-charts -n dfo --create-namespace
```

### 2. 개별 구성요소 배포

```powershell
# Backend만 배포
helm upgrade --install df-party-backend ./backend -n dfo --create-namespace

# Frontend만 배포
helm upgrade --install df-party-frontend ./frontend -n dfo --create-namespace
```

## 설정

### Backend 설정

- **포트**: 8080
- **서비스 타입**: ClusterIP
- **Health Check**: `/actuator/health`
- **데이터베이스**: Infrastructure의 MariaDB 연결
- **이미지 풀 정책**: Always (최신 이미지 자동 배포)

### Frontend 설정

- **포트**: 80
- **서비스 타입**: ClusterIP
- **이미지 풀 정책**: Always (최신 이미지 자동 배포)

## 환경 변수

### Backend 환경 변수

- `DB_HOST`: MariaDB 호스트 (기본값: df-party-mariadb)
- `DB_PORT`: MariaDB 포트 (기본값: 3306)
- `DB_NAME`: 데이터베이스 이름 (기본값: dnfp)
- `DB_USERNAME`: 데이터베이스 사용자 (기본값: root)
- `DB_PASSWORD`: 데이터베이스 비밀번호 (기본값: dfparty-root-password)
- `DF_API_BASE_URL`: DFO API 기본 URL
- `DF_API_RATE_LIMIT`: API 요청 제한
- `DF_API_RATE_LIMIT_WINDOW`: API 요청 제한 시간 창

## 개발 워크플로우

1. **코드 변경**: Backend 또는 Frontend 코드 수정
2. **이미지 빌드**: `.\deploy.ps1` 실행
3. **애플리케이션 재배포**: `.\deploy-application.ps1` 실행
4. **Infrastructure는 유지**: MariaDB와 Nginx는 그대로 유지

## 문제 해결

### Backend Health Check 실패

- `/actuator/health` 엔드포인트가 404를 반환하는 경우
- 애플리케이션 로그 확인: `kubectl logs -f <backend-pod> -n dfo`
- 데이터베이스 연결 상태 확인

### 이미지 업데이트 문제

- `pullPolicy: Always` 설정 확인
- 이미지 태그가 `latest`인지 확인
- Docker 이미지가 정상적으로 푸시되었는지 확인

## 다음 단계

애플리케이션 배포 완료 후, 상태를 확인하세요:

```powershell
# Pod 상태 확인
kubectl get pods -n dfo

# 서비스 상태 확인
kubectl get services -n dfo

# 애플리케이션 로그 확인
kubectl logs -f <pod-name> -n dfo
```
