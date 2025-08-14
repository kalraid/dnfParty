# DFO Party Infrastructure Helm Chart

이 Helm 차트는 DFO Party 애플리케이션의 인프라 구성요소를 관리합니다.

## 구성 요소

- **MariaDB**: 데이터베이스 서버
- **Nginx**: 리버스 프록시 및 로드 밸런서

## 특징

- **안정적인 구성요소**: 자주 변경되지 않는 인프라 구성요소만 포함
- **독립적 배포**: 애플리케이션과 별도로 배포 가능
- **데이터 지속성**: MariaDB 데이터는 PVC를 통해 보존

## 배포 방법

### 1. Infrastructure 배포

```powershell
# PowerShell
.\deploy-infrastructure.ps1 -Force

# 또는 수동으로
helm upgrade --install df-party-infrastructure ./helm-charts-infrastructure -n dfo --create-namespace
```

### 2. 개별 구성요소 배포

```powershell
# MariaDB만 배포
helm upgrade --install df-party-mariadb ./mariadb -n dfo --create-namespace

# Nginx만 배포
helm upgrade --install df-party-nginx ./nginx -n dfo --create-namespace
```

## 설정

### MariaDB 설정

- **데이터베이스**: `dnfp`
- **루트 비밀번호**: `dfparty-root-password`
- **사용자**: `dfparty`
- **사용자 비밀번호**: `dfparty-password`
- **포트**: 3306

### Nginx 설정

- **포트**: 80
- **서비스 타입**: LoadBalancer
- **업스트림 서비스**:
  - Frontend: `df-party-frontend:80`
  - Backend: `df-party-backend:8080`
  - MariaDB: `df-party-mariadb:3306`

## 주의사항

- Infrastructure는 자주 재배포하지 않습니다
- MariaDB 데이터는 PVC를 통해 보존되므로 `-Force` 플래그 사용 시 주의
- Nginx 설정 변경 시 ConfigMap을 수정해야 할 수 있습니다

## 다음 단계

Infrastructure 배포 완료 후, 애플리케이션을 배포하세요:

```powershell
.\deploy-application.ps1
```
