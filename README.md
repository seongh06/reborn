# Re;Born

> 낡은 장비에 새 생명을, 불편한 공간에 스마트함을

<br>

## 📌 프로젝트 소개

**Re;Born**은 Arduino IoT 센서로 실내 온도·습도·조도·재실 인원을 실시간 수집하고,
Android/iOS 앱(Compose Multiplatform) 및 QR 웹페이지를 통해 모니터링·제어하는 **스마트 실내 환경 관리 플랫폼**입니다.

하나의 앱을 두 가지 모드로 운용하는 것이 핵심 아이디어입니다.

- **관리자 모드** — 대시보드로 실내 환경을 확인하고, 피드백을 받고, IoT 기기를 원격 제어
- **공기계 모드** — 방치되어 있던 여분 스마트폰을 "센서 + 로컬 제어 허브"로 재활용(Re;Born)

상용 클라우드 서버 대신 **자체 홈서버(Docker Compose)**를 운영하여 월 고정 비용 없이 서비스합니다.

기획 · 디자인 · 프론트(iOS/Android) · 백엔드 · 인프라 · IoT를 **1인이 전담**하는 풀스택 프로젝트입니다.

<br>

## 🏗️ 시스템 아키텍처

```mermaid
flowchart TD
    classDef hardware fill:#ffffff,stroke:#cccccc,stroke-width:2px,color:#333333
    classDef client fill:#ffffff,stroke:#cccccc,stroke-width:2px,color:#333333
    classDef server fill:#f9f9f9,stroke:#333333,stroke-width:2px,stroke-dasharray: 5 5
    classDef container fill:#ffffff,stroke:#4CAF50,stroke-width:2px,color:#333333
    classDef external fill:#ffffff,stroke:#ff9800,stroke-width:2px,color:#333333
    classDef cicd fill:#ffffff,stroke:#2196F3,stroke-width:2px,color:#333333

    subgraph External_Network ["클라이언트 및 기기"]
        direction TB
        Arduino["Arduino<br>(온습도 측정)"]:::hardware
        AiSpeaker["AI 스피커<br>(음성 피드백)"]:::hardware
        TargetIoT["제어 대상 IoT 기기<br>(에어컨, 조명 등 — SmartThings 등록 기기)"]:::hardware

        subgraph App ["앱 및 웹"]
            QRWeb["정적 웹페이지<br>(QR 스캔 → 온습도 조회 · 피드백)"]:::client
            AdminApp["관리자 휴대폰<br>(대시보드, 제어 명령 발송, 피드백 관리)"]:::client
            KioskApp["공기계 휴대폰<br>(조도/재실 인원 수집 전용)"]:::client
        end
    end

    subgraph Ingress ["네트워크 진입점"]
        Router["Router<br>(공유기 포트포워딩)"]:::hardware
    end

    subgraph HomeServer ["자체 홈서버 - Raspberry Pi 5 (Docker Compose)"]
        direction TB
        Nginx["Nginx<br>(Reverse Proxy)"]:::container
        SpringBoot["Spring Boot + Kotlin<br>(SmartThings 자격증명 직접 보유)"]:::container

        subgraph DB_Cache ["데이터 저장소 및 문서"]
            direction LR
            MySQL["MySQL 8.0"]:::container
            Redis["Redis 7.0"]:::container
            Swagger["Swagger UI"]:::container
        end
    end

    subgraph External_Services ["외부 클라우드 서비스"]
        direction LR
        OAuth["OAuth 2.0<br>(Kakao, Google)"]:::external
        FCM["Firebase<br>(Cloud Messaging)"]:::external
        S3["AWS S3"]:::external
        Slack["Slack Webhook"]:::external
        Gemini["Gemini API<br>(피드백 분석·추천, 음성 인식)"]:::external
        SmartThings["SmartThings Cloud API<br>(IoT 기기 제어 + 온습도 폴링)"]:::external
        Sheets["Google Sheets API<br>(데이터 내보내기)"]:::external
    end

    subgraph CICD ["CI/CD"]
        direction TB
        GitHub["GitHub<br>(main/dev)"]:::cicd
        GHActions["GitHub Actions"]:::cicd
        DockerBuild["Docker Image Build"]:::cicd
    end

    Arduino -- "HTTP POST<br>[Device Key]" --> Router
    AiSpeaker -- "HTTP POST<br>[Device Key, 음성]" --> Router
    QRWeb -- "HTTP POST<br>[QR 세션 토큰]" --> Router

    AdminApp -- "HTTP REST<br>[JWT Token]" --> Router
    KioskApp -- "HTTP POST<br>[deviceId]" --> Router

    Router -- "외부 트래픽 전달" --> Nginx
    Nginx -- "Proxy Pass" --> SpringBoot

    SpringBoot -- "제어 명령 직접 호출" --> SmartThings
    SmartThings -- "클라우드-투-디바이스 제어" --> TargetIoT
    SpringBoot -. "주기적 온습도 폴링<br>(Arduino 미보유 장소)" .-> SmartThings

    SpringBoot -- "R/W" --> MySQL
    SpringBoot -- "Token / Cache" --> Redis
    SpringBoot -- "API 명세" --> Swagger

    SpringBoot -. "@Async 푸시 요청" .-> FCM
    SpringBoot -. "파일 업로드" .-> S3
    SpringBoot -. "알람 발송" .-> Slack
    SpringBoot -. "분석·음성 인식 요청" .-> Gemini
    SpringBoot -. "데이터 내보내기" .-> Sheets
    AdminApp -. "소셜 로그인" .-> OAuth

    GitHub --> GHActions --> DockerBuild
    DockerBuild -. "Image Pull & Deploy" .-> HomeServer
```

> `/ws/control` WebSocket 중계 방식은 폐기되었습니다. SmartThings OAuth가 PKCE(공개 클라이언트)를 지원하지
> 않아 client_secret이 서버에만 있어야 하고, 이 때문에 **서버가 장소별 SmartThings 자격증명을 직접 보유하고
> API를 동기 호출**하는 구조로 변경되었습니다 — 공기계 앱은 IoT 제어 흐름에서 완전히 제외되고,
> 조도/재실 인원 수집 전용 역할만 남았습니다.

### 핵심 데이터 흐름 — QR 피드백 → 관리자 승인 → IoT 제어

```mermaid
sequenceDiagram
    participant V  as 방문자 (QR 웹)
    participant S  as Spring Boot 서버
    participant F  as FCM
    participant G  as Gemini API
    participant AM as 관리자 앱
    participant ST as SmartThings Cloud
    participant AC as IoT (에어컨)

    V  ->> S  : POST /api/feedback (덥다 / 춥다)
    S  ->> S  : feedback 테이블 저장
    S  ->> F  : 알림 트리거 (관리자 fcmToken)
    F  ->> AM : Push 알림 "피드백 도착"
    S  -->> G : (비동기) 맞춤 온도 추천 분석 요청

    AM ->> S  : IoT 기기 제어 명령 (REST)
    S  ->> ST : SmartThings REST API 직접 호출 (디바이스 커맨드)
    ST ->> AC : 클라우드-투-디바이스 제어 (에어컨 온도 조절 등)
```

<br>

## ✨ 주요 기능

| 기능 | 설명 |
|------|------|
| **실시간 환경 모니터링** | Arduino/SmartThings 센서로 온도·습도·조도·재실 인원 수집 및 앱 대시보드 표시, 불쾌지수 자동 계산 |
| **QR 피드백 시스템** | 방문자가 QR 스캔만으로 현재 환경 확인 및 불편 사항 피드백 제출 (세션당 1회 제한) |
| **AI 스피커 음성 피드백** | QR 없이 음성으로도 피드백 제출, Gemini가 음성 인식·응답 |
| **FCM 푸시 알림** | 피드백 도착 시 관리자 앱으로 실시간 Push 알림 발송 |
| **IoT 원격/자동 제어** | 관리자 앱 → 서버가 **SmartThings API를 직접 호출**해 실제 기기(에어컨 등) 제어. 조건 기반 자동 제어 규칙 실행 지원 |
| **AI 분석 리포트** | 방문자 피드백·환경 데이터를 Gemini가 분석해 요약 제공 — 토큰 절약을 위해 자동 호출 대신 사용자가 직접 열람을 요청할 때만 호출 |
| **Google Sheets 내보내기** | 수집된 환경 데이터를 장소별로 연동된 Google Sheets로 내보내기 |
| **기기 사전 발급 시리얼** | 판매용 아두이노/AI 스피커에 사전 인쇄된 시리얼로 등록 (device_serial 재고 관리) |
| **장소 방장(Owner) 권한** | 장소를 등록한 관리자가 방장이 되어 장소 삭제·방장 위임 권한을 가짐, 그 외 관리자는 나가기만 가능 |
| **최초 접속 튜토리얼** | 신규 관리자에게 화면별 핵심 기능(SmartThings 연결, 기기 등록 등)을 코치마크로 안내 |
| **단일 앱 이중 모드** | 하나의 앱에서 관리자 모드 / 공기계 모드 선택 운용 |
| **소셜 로그인** | Kakao / Google OAuth 2.0 로그인 지원 |
| **자체 홈서버 운영** | Raspberry Pi 5 + Docker Compose 기반 자체 인프라로 상용 클라우드 비용 없이 운영 |

<br>

## 🛠️ 기술 스택

### 모바일 앱
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-000000?style=flat-square&logo=apple&logoColor=white)

### 백엔드
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)

### 인프라
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)
![Raspberry Pi](https://img.shields.io/badge/Raspberry_Pi-A22846?style=flat-square&logo=raspberrypi&logoColor=white)

> 홈서버는 Raspberry Pi 5에서 Docker Compose로 운영합니다. self-hosted GitHub Actions 러너를 파이 위에
> 직접 띄워, dev 브랜치 푸시마다 백엔드/APK가 자동 빌드·배포됩니다.

### 외부 연동 · AI
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=white)
![Google Gemini](https://img.shields.io/badge/Gemini-8E75B2?style=flat-square&logo=googlegemini&logoColor=white)
![Kakao](https://img.shields.io/badge/Kakao-FFCD00?style=flat-square&logo=kakaotalk&logoColor=black)
![Google](https://img.shields.io/badge/Google-4285F4?style=flat-square&logo=google&logoColor=white)
![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=flat-square&logo=amazons3&logoColor=white)

### IoT
![Arduino](https://img.shields.io/badge/Arduino-00878A?style=flat-square&logo=arduino&logoColor=white)
![SmartThings](https://img.shields.io/badge/SmartThings-15BFFF?style=flat-square&logo=samsung&logoColor=white)

<br>

## 📦 프로젝트 구조

```
reborn/
├── build-logic/          # Convention Plugin (Gradle DSL) — application/compose/feature/library
├── composeApp/           # 앱 진입점 — 모드 선택(관리자/공기계)
├── core/
│   ├── common            # 유틸, 권한 처리
│   ├── data               # Repository 구현체
│   ├── datastore           # DataStore(Okio 기반) — 토큰/튜토리얼 완료 상태 로컬 저장
│   ├── designsystem       # 컬러, 타이포, 컴포넌트 토큰
│   ├── domain             # UseCase, Repository 인터페이스
│   ├── model               # 도메인 데이터 클래스
│   ├── navigation          # 앱 네비게이션 정의
│   ├── network             # Ktor 클라이언트
│   └── ui                  # 공용 UI 컴포넌트 (튜토리얼 오버레이 등)
├── feature/
│   ├── intro              # 모드 선택 · 소셜 로그인 · 페어링/초대 코드
│   ├── aerometer          # 공기계 모드 (조도/재실 인원 수집 전용)
│   └── admin/
│       ├── home            # 대시보드
│       ├── data             # 센서 로그 조회 · AI 분석 · Sheets 내보내기
│       ├── feedback         # 피드백 관리
│       ├── adjust           # IoT 원격/자동 제어
│       └── setting          # 장소·기기·계정 설정 (방장 위임/나가기 포함)
└── server/               # Spring Boot 백엔드
    ├── deployment/migration/  # 운영 DB 수동 ALTER 스크립트 (schema.sql은 신규 생성만 담당)
    ├── global/           # 전역 설정 (JWT, Redis, Swagger, 예외처리 등)
    └── domain/           # 기능별 도메인 (auth, place, device, metric, feedback, smartthings, googlesheets)
```

<br>

## 🚀 실행 방법

### 사전 요구사항

- JDK 17 이상
- Android Studio (Hedgehog 이상)
- Docker Desktop
- Kotlin 1.9 이상

### 서버 실행 (로컬)

```bash
# MySQL + Redis 컨테이너 실행
docker-compose up -d database redis

# 서버 실행
./gradlew :server:bootRun
```

### 앱 실행

```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS (Mac 환경)
./gradlew :composeApp:iosDeployIphone
```

<br>

## 🌿 브랜치 & 커밋 컨벤션

```
main      ← 실서비스 배포 (GitHub Actions 자동 배포)
└── dev   ← 개발 통합 및 검증
     └── feature/{영역}-{기능}   (예: feature/server-jwt-auth)
```

| 브랜치 prefix | 용도 |
|--------------|------|
| `feature/` | 새로운 기능 개발 |
| `fix/` | 버그 수정 |
| `refactor/` | 기능 변경 없는 구조 개선 |
| `chore/` | 설정, 환경, 의존성 |

커밋 메시지: `{영역}/{타입}#{이슈번호}: 작업 내용` (예: `SERVER/FEAT#41: domain-device 등록 API 구현`)

<br>

## 👤 개발자

1인 풀스택 개발 — 기획 · 디자인 · iOS/Android 앱 · 백엔드 · 인프라 · IoT 전 영역을 직접 담당했습니다.

| 역할 | 담당 |
|------|------|
| PM / 기획 | 본인 |
| UI/UX 디자인 | 본인 |
| iOS / Android 앱 | 본인 |
| 백엔드 서버 | 본인 |
| 인프라 / DevOps | 본인 |
| IoT 펌웨어 | 본인 |

<br>

---

<p align="center">
  <b>Re;Born</b> — Kotlin Multiplatform · Spring Boot · Arduino · SmartThings
</p>
