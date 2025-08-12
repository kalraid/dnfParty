#!/bin/bash

# DFO Party Management Application Helm Deployment Script
set -e

echo "🚀 DFO Party Management Application Helm 배포 시작..."

# 환경 변수 확인
if [ -z "$DF_API_KEY" ]; then
    echo "⚠️  DF_API_KEY 환경 변수가 설정되지 않았습니다."
    echo "   export DF_API_KEY=your_api_key_here"
    exit 1
fi

# Kubernetes 네임스페이스 생성
NAMESPACE="dfo-party"
echo "📁 Kubernetes 네임스페이스 생성: $NAMESPACE"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# DFO API Secret 생성
echo "🔐 DFO API Secret 생성 중..."
kubectl create secret generic dfo-api-secret \
    --from-literal=api-key="$DF_API_KEY" \
    --namespace=$NAMESPACE \
    --dry-run=client -o yaml | kubectl apply -f -

# Helm 차트 의존성 업데이트
echo "📦 Helm 차트 의존성 업데이트 중..."
cd helm-charts/dfo-party
helm dependency update
cd ../..

# Helm 차트 배포
echo "🚀 Helm 차트 배포 중..."
helm upgrade --install dfo-party ./helm-charts/dfo-party \
    --namespace=$NAMESPACE \
    --set global.environment=production \
    --set backend.env[0].valueFrom.secretKeyRef.name=dfo-api-secret \
    --wait \
    --timeout=10m

# 배포 상태 확인
echo "📊 배포 상태 확인 중..."
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE

# 서비스 URL 출력
echo ""
echo "🎉 Helm 배포 완료!"
echo ""
echo "📱 접속 정보:"
echo "   - 프론트엔드: http://localhost (LoadBalancer IP)"
echo "   - 백엔드 API: http://localhost/api"
echo "   - Mock API: http://localhost/mock"
echo ""
echo "🔧 관리 명령어:"
echo "   - 배포 상태: kubectl get pods -n $NAMESPACE"
echo "   - 서비스 상태: kubectl get services -n $NAMESPACE"
echo "   - 로그 확인: kubectl logs -f deployment/dfo-party-frontend -n $NAMESPACE"
echo "   - Helm 업그레이드: helm upgrade dfo-party ./helm-charts/dfo-party -n $NAMESPACE"
echo "   - Helm 제거: helm uninstall dfo-party -n $NAMESPACE"
