# 던전앤파이터 파티 구성 사이트

던전앤파이터 API를 활용하여 캐릭터 검색, 정보 수집, 파티 구성을 자동화하는 웹사이트입니다.

## 🏗️ 프로젝트 구조

```
game/
├── df-party-frontend/     # Vue.js 프론트엔드
├── df-party-backend/      # Spring Boot 백엔드
├── TODO.md               # 개발 계획서
└── README.md             # 프로젝트 설명서
```

## 🚀 시작하기

### 프론트엔드 (Vue.js)

```bash
cd df-party-frontend
npm install
npm run dev
```

프론트엔드는 `http://localhost:5173`에서 실행됩니다.

### 백엔드 (Spring Boot)

```bash
cd df-party-backend
./mvnw spring-boot:run
```

백엔드는 `http://localhost:8080`에서 실행됩니다.

## 📋 주요 기능

1. **캐릭터 검색 페이지**: 서버별 캐릭터 검색 및 정보 수집
2. **캐릭터 리스트 페이지**: 모험단별 캐릭터 관리 및 던전 클리어 현황
3. **파티 짜기 페이지**: 자동/수동 파티 구성 및 최적화

## 🛠️ 기술 스택

- **Frontend**: Vue 3 + TypeScript + Pinia + Vue Router
- **Backend**: Spring Boot 3.2.0 + Spring Data JPA + H2 Database
- **API**: 던전앤파이터 공식 API

## 📝 개발 계획

자세한 개발 계획은 [TODO.md](./TODO.md)를 참조하세요.

## 🔧 환경 설정

### 필수 요구사항

- Node.js 20.19.0 이상
- Java 17 이상
- Maven 3.6 이상

### 환경 변수

백엔드에서 사용하는 환경 변수:

```bash
DF_API_KEY=your_dungeon_fighter_api_key
```

## 📚 API 문서

- **던전앤파이터 API**: [https://developers.neople.co.kr/contents/apiDocs/df](https://developers.neople.co.kr/contents/apiDocs/df)
- **백엔드 API**: `http://localhost:8080/swagger-ui.html` (개발 완료 후)

## 🤝 기여하기

1. 이 저장소를 포크합니다
2. 기능 브랜치를 생성합니다 (`git checkout -b feature/amazing-feature`)
3. 변경사항을 커밋합니다 (`git commit -m 'Add some amazing feature'`)
4. 브랜치에 푸시합니다 (`git push origin feature/amazing-feature`)
5. Pull Request를 생성합니다

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## ⚠️ 주의사항

- 던전앤파이터 API 사용 시 이용약관을 준수해주세요
- API 호출 제한을 준수해주세요
- 개인정보 보호에 유의해주세요 