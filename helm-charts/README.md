# DFO Party Helm Chart

## 개요
DFO Party Management Application을 위한 Helm 차트입니다. Frontend, Backend, Nginx, MariaDB, Node Exporter를 포함합니다.

## 구성 요소
- **Frontend**: Vue.js 기반 웹 애플리케이션
- **Backend**: Spring Boot 기반 API 서버
- **Nginx**: 리버스 프록시 및 로드 밸런서
- **MariaDB**: 메인 데이터베이스
- **Node Exporter**: 시스템 메트릭 수집
- **NodePort Service**: 외부 접근을 위한 NodePort 타입 서비스

## 데이터베이스 구조
- **dnfp_dev**: 개발용 데이터베이스
- **dnfp_prod**: 운영용 데이터베이스

## 설치 방법

### 1. 의존성 업데이트
```bash
helm dependency update
```

### 2. 기본 설치
```bash
helm install dfo-party . --namespace dfo-party --create-namespace
```

### 3. 개발용 설정으로 설치
```bash
helm install dfo-party . \
  --namespace dfo-party \
  --create-namespace \
  --set global.environment=development \
  --set backend.env[4].value=dnfp_dev \
  --set backend.env[8].value=true
```

### 4. 운영용 설정으로 설치
```bash
helm install dfo-party . \
  --namespace dfo-party \
  --create-namespace \
  --set global.environment=production \
  --set backend.env[4].value=dnfp_prod \
  --set backend.env[8].value=false
```

## 환경변수 설정

### 개발용 환경변수
```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=dnfp_dev
export DB_USERNAME=dfparty
export DB_PASSWORD=dfparty-password
export SPRING_PROFILES_ACTIVE=development
```

### 운영용 환경변수
```bash
export DB_HOST=your-mariadb-host
export DB_PORT=3306
export DB_NAME=dnfp_prod
export DB_USERNAME=dfparty
export DB_PASSWORD=your-secure-password
export SPRING_PROFILES_ACTIVE=production
```

## 데이터베이스 초기화
MariaDB는 Helm 차트 배포 시 자동으로 초기화됩니다. `mariadb-init-configmap.yaml`에 정의된 스크립트가 실행되어 필요한 테이블과 인덱스를 생성합니다.

## 모니터링
Node Exporter가 활성화되어 시스템 메트릭을 수집합니다. Prometheus와 연동하여 사용할 수 있습니다.

## NodePort Service
외부에서 직접 접근할 수 있는 NodePort 타입의 서비스가 제공됩니다.

### 포트 매핑
- **HTTP**: 30080 → 80 (Frontend)
- **HTTPS**: 30443 → 443 (Frontend)
- **Backend API**: 30081 → 8080 (Backend)
- **MariaDB**: 30306 → 3306 (Database)

### 접근 방법
```bash
# Frontend 접근
curl http://<NODE_IP>:30080

# Backend API 접근
curl http://<NODE_IP>:30081/api/health

# MariaDB 접근 (외부 클라이언트에서)
mysql -h <NODE_IP> -P 30306 -u dfparty -p dnfp_dev
```

## 문제 해결

### 데이터베이스 연결 문제
1. MariaDB Pod 상태 확인: `kubectl get pods -n dfo-party`
2. MariaDB 로그 확인: `kubectl logs -n dfo-party deployment/dfo-party-mariadb`
3. 데이터베이스 접속 테스트: `kubectl exec -it -n dfo-party deployment/dfo-party-mariadb -- mysql -u dfparty -p dnfp_dev`

### 애플리케이션 로그 확인
```bash
kubectl logs -n dfo-party deployment/dfo-party-backend
kubectl logs -n dfo-party deployment/dfo-party-frontend
```

## 업그레이드
```bash
helm upgrade dfo-party . --namespace dfo-party
```

## 제거
```bash
helm uninstall dfo-party --namespace dfo-party
kubectl delete namespace dfo-party
```
