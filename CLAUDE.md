# ReBorn — CLAUDE.md

> 이 파일은 Claude Code가 ReBorn 프로젝트를 이해하기 위한 컨텍스트 파일입니다.
> 코드 작성 전 반드시 이 파일을 숙지하고 작업하세요.

---

## 📌 프로젝트 개요

- **프로젝트명:** ReBorn (Re;Born)
- **목적:** Arduino IoT 센서로 실내 온습도·조도·재실 인원을 수집하고, Android/iOS 앱 및 QR 웹페이지로 모니터링·제어하는 스마트 실내 환경 관리 플랫폼
- **인프라:** Raspberry Pi 홈서버 (Docker Desktop), 공유기 포트포워딩, 구매 도메인
- **개발 방식:** 1인 풀스택 개발 (PM · 디자이너 · 프론트 · 백엔드 · 인프라 · IoT 전담)
- **개발 기간:** Phase 0 (~05.31 사전 세팅) / Phase 1~10 (06.01 ~ 07.31)

---

## 📦 멀티모듈 구조

```
reborn/                             ← 루트 프로젝트 (모노레포)
├── build-logic/                    ← Convention Plugin (Gradle DSL)
│   ├── application                 ← 앱 진입점용 convention
│   ├── compose                     ← Compose 설정 convention
│   ├── feature                     ← feature 모듈 공통 convention
│   └── library                     ← 순수 라이브러리 convention
│
├── composeApp/                     ← 앱 진입점 (AAR)
│                                     모드 선택 화면 (관리자 / 공기계)
│
├── core/                           ← 공통 기반 모듈
│   ├── common                      ← 유틸, 확장함수
│   ├── data                        ← Repository 구현체
│   ├── designsystem                ← 색상, 타이포, 컴포넌트 토큰
│   ├── domain                      ← UseCase, Repository 인터페이스
│   ├── model                       ← 도메인 데이터 클래스
│   ├── navigation                  ← 앱 네비게이션 정의
│   ├── network                     ← Ktor 클라이언트
│   ├── ui                          ← 공용 UI 컴포넌트
│   ├── notification                ← (예정) FCM 수신 처리
│   └── datastore                   ← (예정) Proto DataStore
│
├── feature/                        ← 화면 단위 기능 모듈
│   ├── intro                       ← 모드 선택 · 소셜 로그인
│   ├── aerometer                   ← 공기계 모드 (센서 수집 · IoT 제어)
│   └── admin/
│       ├── home                    ← 대시보드 (온습도·조도·재실 현황)
│       ├── data                    ← 센서 로그 조회
│       ├── feedback                ← 피드백 목록 · 승인/거절
│       ├── adjust                  ← IoT 제어 명령 발송
│       └── setting                 ← 앱 설정
│
└── server/                         ← Spring Boot 백엔드 (Kotlin)
    ├── global/                     ← 전역 공통 설정 및 인프라
    │   ├── async                   ← 비동기 처리 (@Async, ThreadPoolTaskExecutor)
    │   ├── handler                 ← 전역 예외 처리 (GlobalExceptionHandler)
    │   ├── jpa                     ← JPA 공통 설정 (BaseEntity, Auditing)
    │   ├── log                     ← 로깅 설정 (MDC)
    │   ├── model                   ← 공통 응답 모델 (ApiResponse, ErrorResponse)
    │   ├── redis                   ← Redis 설정 및 공통 유틸
    │   ├── s3                      ← S3 파일 업로드 설정
    │   ├── slack                   ← Slack Webhook 알림
    │   ├── swagger                 ← SpringDoc OpenAPI 설정
    │   ├── token                   ← JWT 생성·검증·파싱 (JwtProvider, JwtFilter)
    │   ├── util                    ← 공통 유틸 클래스
    │   └── web                     ← CORS, Interceptor, WebMvcConfigurer
    │
    └── domain/                     ← 기능별 비즈니스 도메인
        ├── admin/                  ← 관리자 기능
        ├── auth/                   ← 인증 (OAuth 2.0, JWT)
        ├── data/                   ← 센서 데이터 수집·조회
        ├── feedback/               ← 피드백 관리·FCM 알림
        ├── place/                  ← 장소 관리
        └── device/                 ← 기기(Arduino·공기계) 관리
```

### 도메인 레이어 구조 (domain 하위 공통)

```
{domain}/
├── controller      ← @RestController, 엔드포인트 정의, ApiResponse 반환
├── converter       ← Entity ↔ DTO 변환 (정적 메서드)
├── dto             ← Request / Response static inner class
└── service         ← @Service @Transactional, 비즈니스 로직
                      (추후 CommandService / QueryService 분리 예정)
```

---

## 🗄️ 데이터베이스 스키마

### MySQL 테이블 요약

| 테이블 | 설명 | 주요 변경 |
|--------|------|----------|
| `user` | 사용자 | refreshToken 없음 → Redis 관리 |
| `place` | 장소 | qrCode UNIQUE 추가 |
| `user_place_mapping` | 사용자-장소 권한 (ADMIN/USER) | - |
| `device` | 기기 (ARDUINO/KIOSK) | deviceType, appToken, isOnline 추가 |
| `sensorLogs` | 센서 수집 로그 | (deviceId, createdAt DESC) 인덱스 |
| `feedback` | 방문자 피드백 | userAgent, sessionToken 추가 |

### 테이블 관계

```
user ──< user_place_mapping >── place ──< device ──< sensorLogs
                                                 └──< feedback
```

### Redis 관리 항목

| Key 패턴 | Value | TTL | 용도 |
|----------|-------|-----|------|
| `refresh:{userId}` | refreshToken | 14일 | JWT RefreshToken |
| `session:qr:{sessionToken}` | placeId | 1시간 | QR 접속 임시 세션 |

---

## 🌐 API 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/kakao` | 카카오 소셜 로그인 | ❌ |
| POST | `/api/auth/google` | 구글 소셜 로그인 | ❌ |
| POST | `/api/auth/refresh` | AccessToken 재발급 | ❌ |
| POST | `/api/collect` | 센서 데이터 수집 (Arduino) | Device Key |
| GET | `/api/current` | 특정 기기 최신 센서 데이터 조회 | ❌ |
| GET | `/api/data/history` | 센서 로그 히스토리 조회 | ✅ |
| POST | `/api/feedback` | 피드백 제출 (QR 웹) | ❌ |
| GET | `/api/feedback` | 피드백 목록 조회 | ✅ ADMIN |
| PATCH | `/api/feedback/{id}` | 피드백 상태 변경 | ✅ ADMIN |
| GET | `/api/place` | 장소 목록 조회 | ✅ |
| POST | `/api/place` | 장소 등록 | ✅ ADMIN |
| GET | `/api/device` | 기기 목록 조회 | ✅ |
| POST | `/api/device` | 기기 등록 | ✅ ADMIN |
| WS | `/ws/control` | WebSocket 제어 명령 중계 | ✅ |

---

## 🔄 핵심 데이터 흐름

### 센서 수집 흐름
```
Arduino → POST /api/collect → server/domain/data → sensorLogs 저장
```

### 피드백 → FCM 알림 흐름
```
QR 웹 → POST /api/feedback → feedback 저장
      → (async) FCM 알림 → 관리자 앱 Push
```

### 실시간 IoT 제어 흐름
```
관리자 앱 → WebSocket(/ws/control) → 서버 중계
         → 공기계 앱(KIOSK) → IoT 기기 Wi-Fi 제어
```

---

## 🔐 인증 구조

- **소셜 로그인:** Kakao / Google OAuth 2.0
- **토큰:** JWT AccessToken (단기) + RefreshToken (Redis, 14일)
- **권한:** `user_place_mapping.accessLevel` (ADMIN / USER)
- **공기계 앱:** deviceId 기반 인증 (JWT 아님)

---

## ⚙️ 개발 환경 및 주요 명령어

### 서버 실행

```bash
# :server 모듈만 실행
./gradlew :server:bootRun

# 전체 빌드
./gradlew build

# 테스트
./gradlew :server:test

# 특정 모듈 빌드
./gradlew :core:network:build
./gradlew :feature:admin:home:build
```

### Docker

```bash
# 로컬 MySQL + Redis 실행
docker-compose up -d mysql redis

# 전체 스택 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f server
```

---

## 🌿 브랜치 전략

```
main                          ← 실서비스 배포 (GitHub Actions 트리거)
└── dev                       ← 개발 통합 · 검증
     └── feature/{영역}-{기능} ← 기능 단위 작업
```

### 브랜치 네이밍

```
feature/server-collect-api
feature/server-db-jpa
feature/auth-kakao-oauth
feature/app-intro-screen
feature/app-admin-dashboard
feature/app-aerometer-mode
feature/infra-docker-compose
fix/server-sensor-null-crash
hotfix/fcm-token-missing
chore/gitignore-setup
```

### PR 흐름

```
feature/* → dev (기능 완성 시)
dev → main (Phase 완료 시, GitHub Actions 자동 배포 트리거)
```

---

## 📝 커밋 컨벤션

```
feat(scope)     : 새로운 기능
fix(scope)      : 버그 수정
refactor(scope) : 리팩토링
chore(scope)    : 빌드, 설정, 의존성
docs            : 문서 수정
style(scope)    : 코드 포맷
test(scope)     : 테스트 코드
```

**scope 예시**

```
feat(server): POST /api/collect 엔드포인트 구현
feat(app): 관리자 대시보드 화면 구현
fix(server): sensorLogs null 저장 오류 수정
chore(infra): docker-compose.yml 초안 작성
refactor(server): AdminService Command/Query 분리
```

---

## 🎨 코드 스타일 규칙

### Kotlin 공통

- 들여쓰기: 4 spaces
- 최대 줄 길이: 120자
- `data class` 사용 우선 (DTO, Model)
- `?` nullable 최소화, `!!` 사용 금지
- `when` 표현식 exhaustive하게 작성

### Spring Boot (server 모듈)

- Controller는 얇게 — 비즈니스 로직은 Service에
- `@Transactional(readOnly = true)` 조회 메서드에 반드시 적용
- `ApiResponse<T>` 공통 응답 래퍼 사용
- Converter는 `static` 메서드로만 작성 (빈 등록 X)
- DTO는 inner static class로 `Request` / `Response` 분리
- 예외는 `BusinessAlertException` + `CommonErrorCode` 사용

### Compose Multiplatform (app 모듈)

- 화면 단위 Composable은 feature 모듈에 위치
- 공용 컴포넌트는 `core:ui` 또는 `core:designsystem`에 위치
- ViewModel은 `core:domain` UseCase만 주입
- `core:network`의 클라이언트를 직접 feature에서 호출 금지

---

## 🚀 현재 개발 Phase

> 코드 작성 시 현재 Phase에 맞는 구현 범위를 지켜주세요.

| Phase | 기간 | 내용 | 상태 |
|-------|------|------|------|
| Phase 0 | ~05.31 | 설계 · 환경 세팅 | ✅ |
| Phase 1 | 06.01~06.07 | MVP 백엔드 (메모리 기반 API) | 🔲 |
| Phase 2 | 06.08~06.14 | DB 연동 (MySQL JPA) | 🔲 |
| Phase 3 | 06.15~06.21 | 인증 (OAuth 2.0, JWT) | 🔲 |
| Phase 4 | 06.22~06.28 | 공기계 모드 앱 | 🔲 |
| Phase 5 | 06.29~07.05 | 관리자 모드 앱 | 🔲 |
| Phase 6 | 07.06~07.12 | 피드백 + FCM 알림 | 🔲 |
| Phase 7 | 07.13~07.19 | WebSocket 실시간 제어 | 🔲 |
| Phase 8 | 07.20~07.24 | 홈서버 배포 (Raspberry Pi) | 🔲 |
| Phase 9 | 07.25~07.28 | CI/CD (GitHub Actions) | 🔲 |
| Phase 10 | 07.29~07.31 | 고도화 · 최종 점검 | 🔲 |

---

## ⚠️ 주의 사항

- `application-local.yml`, `.env`, `google-services.json`, `GoogleService-Info.plist` 는 절대 커밋 금지
- `main` 브랜치 직접 push 금지 — 반드시 PR 통해서 병합
- DB 스키마 변경 시 이 파일의 스키마 섹션도 함께 업데이트
- Phase 완료 시 위 로드맵 테이블의 상태(🔲 → ✅) 업데이트
