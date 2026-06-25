# Jiber Web

지도 기반 부동산 탐색, 실거래 데이터 분석, 아파트 적정가 추정, SHAP 기반 가격 설명을 제공하는 부동산 거래 정보 웹 플랫폼입니다.

이 프로젝트는 사용자가 실거래 정보와 가격 설명을 이해하도록 돕는 데이터 서비스입니다. 투자 조언, 매수/매도 판단, 수익률 보장, 특정 부동산 추천은 범위 밖입니다.

## 제품 언어 원칙

- 사용자에게 보이는 웹 UI 문구는 자연스러운 한국어로 작성합니다.
- 화면 문구, 메뉴, 버튼, 라벨, 안내문, 에러 메시지, 빈 상태 문구는 한국어를 기본으로 합니다.
- 기술 식별자, 코드, API 경로, 설정 키는 영어를 유지할 수 있습니다.

## 기술 스택

- Frontend: Vue 3, Vite, Vue Router, Pinia, Axios, Kakao Maps API, ECharts
- Backend: Spring Boot, MySQL, MyBatis, Spring Security, OAuth2 Login, JWT, Springdoc OpenAPI, Bean Validation
- AI: FastAPI, Hedonic Price Model, SHAP 기반 XAI
- Data: MySQL schema, seed/import scripts, model feature mapping

## 디렉터리 구조

```text
backend/        Spring Boot API 서버
frontend/       Vue 3 SPA
model-server/   FastAPI 모델 서버
db/             MySQL 스키마, seed, migration 예정 영역
docs/
  architecture/ 시스템 설계 문서
  contracts/    서비스 간 계약 문서
  api/          API 문서와 예시
  model/        모델/데이터 문서
  security/     인증/인가 문서
  qa/           검증 계획과 리뷰 문서
.agents/        프로젝트 로컬 Codex 에이전트 정의
```

## 로컬 실행 방식

처음 프로젝트를 받는 경우에는 아래 순서로 실행합니다.

```bash
git clone https://github.com/dongkyu20/Jiber_Web.git
cd Jiber_Web

cp .env.example .env
# .env에서 DB_PASSWORD, DB_ROOT_PASSWORD 등 필수 로컬 값을 채웁니다.

docker compose up --build -d

cd frontend
npm install
npm run dev
```

실행 후 브라우저에서 Vite가 출력하는 주소, 기본적으로 `http://localhost:5173`, 로 접속합니다. Docker Compose는 MySQL, DB dump/import, model-server, backend를 백그라운드로 실행합니다. 프론트엔드는 `npm run dev`로 별도 실행합니다.

Phase 1 로컬 개발 흐름은 다음 순서입니다.

1. `.env.example`을 `.env`로 복사하고 `DB_PASSWORD`, `DB_ROOT_PASSWORD`를 로컬 값으로 채웁니다.
2. `docker compose up -d mysql`로 MySQL 8을 실행합니다.
3. 처음 생성되는 Docker volume에는 `db/001_phase1_schema.sql`, `db/002_public_data_import.sql`, `db/003_seed_sample_properties.sql`, `db/004_auth_account_social_link.sql`, `db/005_property_transaction_source_unique.sql`이 순서대로 적용됩니다.
4. auth UX smoke 전에는 `scripts/check-auth-schema.sh`를 실행해 오래된 local DB volume에 004 auth migration이 빠져 있지 않은지 확인합니다. 이 preflight는 schema만 읽고 secret 값을 출력하지 않습니다.
5. DB-backed smoke test 전에는 Docker published port와 `.env`의 `DB_PORT`가 일치하는지 확인합니다. 자세한 MySQL smoke 예시는 `backend/README.md`를 참고합니다.
6. root `.env`를 process env로 주입하는 dev script로 backend와 frontend를 실행합니다.
7. `model-server/`에서 FastAPI 모델 서버를 실행합니다.
8. 프론트엔드는 Spring Boot API의 `/api/v1/**`만 호출하고, Spring Boot가 내부적으로 모델 서버를 호출합니다.

Docker Compose로 전체 백엔드 스택을 띄우면 공개 Google Drive 자산도 자동으로 내려받습니다.

```bash
cp .env.example .env
# .env에서 DB_PASSWORD, DB_ROOT_PASSWORD 등 로컬 secret 값을 채웁니다.
docker compose up --build
```

`model-assets` 서비스가 가격예측 feature dataset, 가격예측 모델 zip, XAI 산출물을 로컬 artifact 디렉터리에 저장한 뒤 `model-server`와 `backend`가 이어서 시작됩니다. 모델 zip은 Google Drive에 zip 파일 링크로 둬도 됩니다. Compose 실행 중 `scripts/download-google-drive-assets.py`가 zip을 내려받아 `VALUATION_ARTIFACTS_DIR`에 압축해제합니다.

Google Drive 폴더 링크는 폴더만 보이는 상태로는 부족합니다. 폴더 안의 각 파일도 `링크가 있는 모든 사용자`가 다운로드할 수 있어야 compose 자동 다운로드가 끝까지 성공합니다. 스크립트는 Drive 폴더 목록은 `gdown`으로 읽고, 개별 파일은 Drive content URL로 직접 내려받아 브라우저에서는 되지만 `gdown` 단독으로는 실패하는 공개 파일도 처리합니다. 이미 받은 자산을 다시 받고 싶으면 `.env`에서 `MODEL_ASSETS_FORCE_DOWNLOAD=true`로 설정한 뒤 `docker compose up model-assets`를 실행합니다. 네트워크 없이 skeleton만 띄워야 하는 경우에는 `MODEL_ASSETS_SKIP_DOWNLOAD=true`를 사용할 수 있습니다. 특정 자산만 건너뛰려면 `MODEL_ASSETS_SKIP_DATASET`, `MODEL_ASSETS_SKIP_MODEL`, `MODEL_ASSETS_SKIP_XAI`를 사용할 수 있습니다.

DB dump도 Compose에서 자동으로 반영됩니다. `db-assets` 서비스가 `DB_DUMP_DRIVE_URL`의 최신 zip dump를 `db/dumps`에 내려받아 압축해제하고, `db-import` 서비스가 MySQL 준비 후 최신 `.sql` dump를 현재 `DB_NAME`에 import합니다. dump는 import 이력 테이블로 한 번만 반영됩니다. 다시 import하려면 `DB_DUMP_FORCE_IMPORT=true`를 설정합니다. DB dump 다운로드/반영을 건너뛰려면 `DB_DUMP_SKIP_DOWNLOAD=true` 또는 `DB_DUMP_SKIP_IMPORT=true`를 사용할 수 있습니다.

권장 dev 실행:

```bash
scripts/dev-backend.sh
scripts/dev-frontend.sh
```

`scripts/dev-backend.sh`는 root `.env`를 읽고 Docker MySQL published port와 `DB_PORT`가 어긋나면 실행 전에 막거나 경고합니다. 기본적으로 `scripts/check-auth-schema.sh`도 실행해 auth schema preflight를 먼저 확인합니다.

Vite config는 root `.env`를 envDir로 읽습니다. `scripts/dev-frontend.sh`는 같은 env를 명시적으로 export하고 API base URL 기본값을 맞춰 주는 편의 스크립트입니다.

Kakao key 용도는 분리되어 있습니다.

- `KAKAO_REST_API_KEY`: backend 공공데이터 지번 주소 geocoding용.
- `VITE_KAKAO_MAP_APP_KEY`: frontend Kakao Maps JavaScript SDK용.

공공데이터포털 실거래 import는 backend batch runner로 실행합니다. 현재 seed 데이터는 map/search/detail API smoke test용 synthetic 데이터이며, live public data import와 Kakao geocoding은 별도 실행 단계입니다. 로컬 개발에서는 Docker MySQL 또는 로컬 MySQL을 사용할 수 있고, 운영에서는 Docker MySQL 또는 managed DB 중 운영 기준에 맞게 선택합니다. 실제 public data service key와 Kakao REST API key는 `.env`에만 둡니다.

실제 API 키, OAuth client secret, JWT secret, DB 비밀번호는 저장소에 커밋하지 않습니다. 필요한 환경 변수 이름은 루트 `.env.example`에만 정의합니다.

## MVP 범위

- 랜딩페이지
- 지도 기반 부동산 검색
- 필터 검색
- 부동산 상세 기본 정보와 최근 실거래 정보
- 소셜 로그인
- 즐겨찾기
- 공지사항, 회원 관리와 관리자 권한
- 아파트 적정가 추정
- SHAP 기반 가격 설명 시각화

MVP에서 제외하는 범위:

- 투자 조언, 매수/매도 판단, 수익률 보장
- 실시간 매물 중개 또는 계약 기능
- 아파트 외 부동산의 AI 가격 추정
- 실제 secret, API key, OAuth client secret의 저장 또는 추정
