#!/bin/bash

# DFO Party Management Application Docker Image Build & Push Script
set -e

echo "🚀 DFO Party Management Application Docker 이미지 빌드 및 푸시 시작..."

# dfo 네임스페이스가 없으면 생성
echo "📁 dfo 네임스페이스가 없으면 생성 중..."
kubectl create namespace dfo --dry-run=client -o yaml | kubectl apply -f -
echo "✅ dfo 네임스페이스 준비 완료"

# 환경 변수 확인
if [ -z "$DF_API_KEY" ]; then
    echo "⚠️  DF_API_KEY 환경 변수가 설정되지 않았습니다."
    echo "   export DF_API_KEY=your_api_key_here"
    echo "   또는 시스템 환경변수에 설정하세요."
    exit 1
fi

echo "✅ DF_API_KEY 환경변수 확인됨"

# Docker 이미지 빌드
echo "📦 Docker 이미지 빌드 중..."

# 프론트엔드 빌드
echo "🔨 프론트엔드 빌드 중..."
cd df-party-frontend
docker build -t kimrie92/dfo-party-frontend:latest .
cd ..

# 백엔드 빌드
echo "🔨 백엔드 빌드 중..."
cd df-party-backend
docker build -t kimrie92/dfo-party-backend:latest .
cd ..

# Mock 백엔드 빌드
echo "🔨 Mock 백엔드 빌드 중..."
cd df-party-mock
./gradlew build -x test
docker build -t kimrie92/dfo-party-mock:latest .
cd ..

echo "✅ 모든 이미지 빌드 완료!"

# Docker 이미지 푸시
echo "📤 Docker 이미지 푸시 중..."

echo "📤 프론트엔드 이미지 푸시 중..."
docker push kimrie92/dfo-party-frontend:latest

echo "📤 백엔드 이미지 푸시 중..."
docker push kimrie92/dfo-party-backend:latest

echo "📤 Mock 백엔드 이미지 푸시 중..."
docker push kimrie92/dfo-party-mock:latest

echo "✅ 모든 이미지 푸시 완료!"

echo "🎉 배포 준비 완료!"
echo ""
echo "📱 다음 단계:"
echo "   - Helm 배포: ./deploy-helm.sh"
