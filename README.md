# Re;Born

> 낡은 장비에 새 생명을, 불편한 공간에 스마트함을

<br>

## 📌 프로젝트 소개

**Re;Born**은 Arduino IoT 센서로 실내 온도·습도·조도·재실 인원을 실시간 수집하고,
iOS/Android 앱 및 QR 웹페이지를 통해 모니터링·제어하는 **스마트 실내 환경 관리 플랫폼**입니다.

상용 클라우드 서버 대신 **Raspberry Pi 홈서버**를 활용하여 월 운영 비용 없이 자체 인프라로 운영합니다.

<br>

## 🏗️ 시스템 아키텍처

![Architecture](docs/architecture.png)

<br>

## ✨ 주요 기능

| 기능 | 설명 |
|------|------|
| **실시간 환경 모니터링** | Arduino 센서로 온도·습도·조도·재실 인원 수집 및 앱 대시보드 표시 |
| **QR 피드백 시스템** | 방문자가 QR 스캔만으로 현재 환경 확인 및 불편 사항 피드백 제출 |
| **FCM 푸시 알림** | 피드백 도착 시 관리자 앱으로 실시간 Push 알림 발송 |
| **IoT 원격 제어** | 관리자 앱 → 서버 → 공기계 앱 → IoT 기기(에어컨 등) WebSocket 실시간 제어 |
| **단일 앱 이중 모드** | 하나의 앱에서 관리자 모드 / 공기계 모드 선택 운용 |
| **소셜 로그인** | Kakao / Google OAuth 2.0 로그인 지원 |
| **AI 보고서 요약** | 기간별 센서 데이터를 AI가 분석하여 환경 개선 제안 제공 |
| **자체 홈서버 운영** | Raspberry Pi + Docker로 클라우드 비용 없이 자체 인프라 운영 |

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
![Raspberry Pi](https://img.shields.io/badge/Raspberry_Pi-A22846?style=flat-square&logo=raspberrypi&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

### IoT
![Arduino](https://img.shields.io/badge/Arduino-00878A?style=flat-square&logo=arduino&logoColor=white)

<br>

## 📦 프로젝트 구조

```
reborn/
├── build-logic/          # Convention Plugin (Gradle DSL)
├── composeApp/           # 앱 진입점 (AAR)
├── core/
│   ├── common
│   ├── data
│   ├── designsystem
│   ├── domain
│   ├── model
│   ├── navigation
│   ├── network
│   └── ui
├── feature/
│   ├── intro             # 모드 선택 · 소셜 로그인
│   ├── aerometer         # 공기계 모드
│   └── admin/
│       ├── home          # 대시보드
│       ├── data          # 센서 로그
│       ├── feedback      # 피드백 관리
│       ├── adjust        # IoT 제어
│       └── setting
└── server/               # Spring Boot 백엔드
    ├── global/           # 전역 설정 (JWT, Redis, Swagger 등)
    └── domain/           # 기능별 도메인 (auth, data, feedback 등)
```

<br>

## 🗄️ ERD

```
user ──< user_place_mapping >── place ──< device ──< sensorLogs
                                                 └──< feedback
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
docker-compose up -d mysql redis

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

## 🌿 브랜치 전략

```
main      ← 실서비스 배포 (GitHub Actions 자동 배포)
└── dev   ← 개발 통합 및 검증
     └── feature/{영역}-{기능명}
```

| 브랜치 prefix | 용도 |
|--------------|------|
| `feature/` | 새로운 기능 개발 |
| `fix/` | 버그 수정 |
| `hotfix/` | 긴급 수정 |
| `chore/` | 설정, 환경, 의존성 |

<br>

## 📅 개발 일정

| Phase | 기간 | 내용 |
|-------|------|------|
| Phase 0 | ~ 05.31 | 설계 및 환경 세팅 |
| Phase 1 | 06.01 ~ 06.07 | MVP 백엔드 (메모리 기반 API) |
| Phase 2 | 06.08 ~ 06.14 | DB 연동 (MySQL JPA) |
| Phase 3 | 06.15 ~ 06.21 | 인증 (OAuth 2.0, JWT) |
| Phase 4 | 06.22 ~ 06.28 | 공기계 모드 앱 |
| Phase 5 | 06.29 ~ 07.05 | 관리자 모드 앱 |
| Phase 6 | 07.06 ~ 07.12 | 피드백 + FCM 알림 |
| Phase 7 | 07.13 ~ 07.19 | WebSocket 실시간 제어 |
| Phase 8 | 07.20 ~ 07.24 | 홈서버 배포 (Raspberry Pi) |
| Phase 9 | 07.25 ~ 07.28 | CI/CD (GitHub Actions) |
| Phase 10 | 07.29 ~ 07.31 | 고도화 및 최종 점검 |

<br>

## 📁 문서

| 문서 | 경로 |
|------|------|
| 아키텍처 다이어그램 | `docs/architecture.md` |
| API 명세서 | `docs/api/` |
| ERD | `docs/erd.md` |
| DDL | `docs/ddl.sql` |
| 개발 로드맵 | `docs/roadmap.md` |
| 개발 일지 | `devlog/` |

<br>

## 👤 개발자

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
  <b>Re;Born</b> — Raspberry Pi · Arduino · Kotlin Multiplatform · Spring Boot
</p>