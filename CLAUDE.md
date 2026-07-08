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
│   └── datastore                   ← DataStore(Okio 기반, KMP 공용) — AccessToken/RefreshToken 로컬 저장
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
        ├── metric/                 ← 실내 환경 지표(온습도·조도·재실 인원) 수집·조회
        ├── feedback/               ← 피드백 관리·FCM 알림
        ├── place/                  ← 장소 관리
        └── device/                 ← 기기(Arduino·공기계) 등록·관리 (페어링 포함)
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
| `user` | 사용자 | refreshToken 없음 → Redis 관리, email nullable(카카오 이메일 동의 미사용) |
| `place` | 장소 | qrCode UNIQUE 추가 |
| `user_place_mapping` | 사용자-장소 권한 (ADMIN/USER) | - |
| `device` | 기기 (ARDUINO/AEROMETER) | deviceType, appToken, isOnline 추가 |
| `metricLogs` | 메트릭(온습도·조도·재실 인원) 수집 로그 | (deviceId, createdAt DESC) 인덱스 |
| `feedback` | 방문자 피드백 | userAgent, sessionToken 추가 |

### 테이블 관계

```
user ──< user_place_mapping >── place ──< device ──< metricLogs
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
| POST | `/api/metric/collect` | 메트릭 수집 (Arduino) | Device Key |
| GET | `/api/metric/current` | 특정 기기 최신 메트릭 조회 | ❌ |
| GET | `/api/metric/history` | 메트릭 히스토리 조회 | ✅ |
| POST | `/api/feedback` | 피드백 제출 (QR 웹) | ❌ |
| GET | `/api/feedback` | 피드백 목록 조회 | ✅ ADMIN |
| PATCH | `/api/feedback/{id}` | 피드백 상태 변경 | ✅ ADMIN |
| GET | `/api/place` | 장소 목록 조회 | ✅ |
| POST | `/api/place` | 장소 등록 | ✅ ADMIN |
| GET | `/api/device` | 기기 목록 조회 | ✅ |
| POST | `/api/device` | Arduino 기기 등록 | ✅ ADMIN |
| POST | `/api/device/pairing/code` | 공기계 페어링 코드 생성 | ✅ ADMIN |
| POST | `/api/device/pairing` | 공기계 페어링 코드 입력·등록 | ✅ |
| WS | `/ws/control` | WebSocket 제어 명령 중계 | ✅ |

---

## 🔄 핵심 데이터 흐름

### 메트릭 수집 흐름
```
Arduino → POST /api/metric/collect → server/domain/metric → metricLogs 저장
```

### 피드백 → FCM 알림 흐름
```
QR 웹 → POST /api/feedback → feedback 저장
      → (async) FCM 알림 → 관리자 앱 Push
```

### 실시간 IoT 제어 흐름
```
관리자 앱 → WebSocket(/ws/control) → 서버 중계
         → 공기계 앱(AEROMETER) → SmartThings API 호출 → IoT 기기 제어
```
> 2026-07-06 확정: 공기계 앱이 SmartThings Cloud API를 직접 호출하여 기기를 제어함(로컬 Wi-Fi 직접 제어 아님). 서버는 SmartThings 자격증명을 갖지 않고 WebSocket 중계까지만 담당.

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

main                           ← 실서비스 배포 (GitHub Actions 트리거)
└── dev                        ← 개발 통합 · 검증
└── feature/{영역}-{기능}  ← 기능 단위 작업 (Epic 하위 작업 단위)


### 브랜치 네이밍 규칙
* **형식:** `{type}/{영역}-{기능}`
* **예시:**
    * `feature/server-jwt-auth`
    * `feature/server-logging-mdc`
    * `feature/app-lck-ranking`
    * `fix/server-token-expired`
    * `refactor/server-security-config`
    * `chore/server-gradle-dependency`

### PR 흐름
* `feature/*` 또는 `fix/*` → `dev` (하위 이슈 또는 단위 기능 완성 시)
* `dev` → `main` (각 Phase 완료 시, GitHub Actions 자동 배포 트리거)

---

## 📝 커밋 컨벤션 (Commit Convention)

### 커밋 메시지 기본 구조
{영역}/{타입}#{Epic번호}-{Sub이슈번호}: {작업 내용 명사형 종결}

* **영역:** `SERVER`, `APP` 등 대문자 표기
* **타입:** `FEAT`, `FIX`, `REFACTOR`, `CHORE`, `DOCS`, `TEST` 등 대문자 표기

### 타입별 정의 및 예시
* **FEAT:** 새로운 기능 구현
    * `SERVER/FEAT#1-4: JWT 인프라 및 보안 필터 구현`
* **FIX:** 버그 수정
    * `SERVER/FIX#2-1: 토큰 만료 예외 처리 오류 수정`
* **REFACTOR:** 리팩토링 (기능 변경 없는 코드 구조 개선)
    * `SERVER/REFACTOR#1-5: SecurityConfig 구조 개선`
* **CHORE:** 빌드 설정, 의존성 추가, 환경변수 변경 등
    * `SERVER/CHORE#1-1: JJWT 라이브러리 의존성 추가`
* **DOCS:** 문서 생성 및 수정 (README, API 문서 등)
    * `SERVER/DOCS#1-2: Swagger 설정 가이드라인 문서 작성`
* **TEST:** 테스트 코드 추가 및 수정
    * `SERVER/TEST#1-4: JwtProvider 단위 테스트 작성`

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
