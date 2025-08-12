#!/bin/bash

# DFO Party Management Application Deployment Script
set -e

echo "🚀 DFO Party Management Application 배포 시작..."

# 환경 변수 확인
if [ -z "$DF_API_KEY" ]; then
    echo "⚠️  DF_API_KEY 환경 변수가 설정되지 않았습니다."
    echo "   export DF_API_KEY=your_api_key_here"
    exit 1
fi

# Docker 이미지 빌드
echo "📦 Docker 이미지 빌드 중..."

# 프론트엔드 빌드
echo "🔨 프론트엔드 빌드 중..."
cd df-party-frontend
docker build -t dfo-party-frontend:latest .
cd ..

# 백엔드 빌드
echo "🔨 백엔드 빌드 중..."
cd df-party-backend
docker build -t dfo-party-backend:latest .
cd ..

# Mock 백엔드 빌드
echo "🔨 Mock 백엔드 빌드 중..."
cd df-party-mock
./gradlew build -x test
docker build -t dfo-party-mock:latest .
cd ..

echo "✅ 모든 이미지 빌드 완료!"

# Docker Compose로 서비스 시작
echo "🐳 Docker Compose로 서비스 시작 중..."
docker-compose up -d

# 서비스 상태 확인
echo "📊 서비스 상태 확인 중..."
sleep 10

# 헬스 체크
echo "🏥 헬스 체크 중..."
curl -f http://localhost/health || echo "❌ Nginx 헬스 체크 실패"
curl -f http://localhost:8080/actuator/health || echo "❌ 백엔드 헬스 체크 실패"
curl -f http://localhost:8081/health || echo "❌ Mock 백엔드 헬스 체크 실패"

echo "🎉 배포 완료!"
echo ""
echo "📱 접속 정보:"
echo "   - 프론트엔드: http://localhost"
echo "   - 백엔드 API: http://localhost/api"
echo "   - Mock API: http://localhost/mock"
echo "   - Nginx 상태: http://localhost/nginx_status"
echo ""
echo "🔧 관리 명령어:"
echo "   - 서비스 중지: docker-compose down"
echo "   - 로그 확인: docker-compose logs -f"
echo "   - 서비스 재시작: docker-compose restart"
