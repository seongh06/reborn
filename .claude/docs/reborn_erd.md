# 🗄️ ReBorn — ERD

```mermaid
erDiagram
    user {
        VARCHAR userId PK "소셜 로그인 고유 ID (kakao_123 / google_456)"
        VARCHAR name "사용자 이름"
        VARCHAR email "이메일 (소셜 제공 시)"
        VARCHAR fcmToken "FCM Push 알림 토큰"
        DATETIME createdAt "가입일시"
    }

    place {
        INT placeId PK "장소 ID (AUTO_INCREMENT)"
        VARCHAR name "장소명"
        VARCHAR placeType "장소 유형 (HOME / STORE / COMPANY)"
        DOUBLE latitude "위도"
        DOUBLE longitude "경도"
        VARCHAR qrCode UK "QR 웹페이지 식별 고유 코드"
    }

    user_place_mapping {
        VARCHAR userId PK,FK "사용자 ID"
        INT placeId PK,FK "장소 ID"
        VARCHAR accessLevel "권한 (ADMIN / USER)"
    }

    device {
        VARCHAR deviceId PK "기기 고유 ID"
        INT placeId FK "장소 ID"
        VARCHAR deviceName "기기 이름 / 방 이름"
        VARCHAR deviceType "기기 유형 (ARDUINO / KIOSK)"
        VARCHAR appToken "공기계 앱 WebSocket 식별자"
        TINYINT isOnline "온라인 여부 (0 / 1)"
        DATETIME createdAt "기기 등록일시"
    }

    sensorLogs {
        BIGINT logId PK "로그 ID (AUTO_INCREMENT)"
        VARCHAR deviceId FK "기기 ID"
        DECIMAL temperature "온도 (°C)"
        DECIMAL humidity "습도 (%)"
        INT illuminance "조도 (lux)"
        INT peopleCount "재실 인원 수"
        DECIMAL discomfort "불쾌지수"
        DATETIME createdAt "수집일시 — INDEX"
    }

    feedback {
        INT feedbackId PK "피드백 ID (AUTO_INCREMENT)"
        VARCHAR deviceId FK "기기 ID"
        VARCHAR title "피드백 제목"
        TEXT content "피드백 내용"
        VARCHAR status "처리 상태 (PENDING / APPROVED / REJECTED)"
        VARCHAR userIp "작성자 IP"
        VARCHAR userAgent "브라우저 User-Agent"
        VARCHAR sessionToken "QR 임시 세션 토큰"
        DATETIME createdAt "작성일시"
    }

    user            ||--o{ user_place_mapping : "가입"
    place           ||--o{ user_place_mapping : "소속"
    place           ||--o{ device             : "설치"
    device          ||--o{ sensorLogs         : "수집"
    device          ||--o{ feedback           : "접수"
```

---

## 📦 Redis 관리 항목

| Key 패턴 | Value | TTL | 용도 |
|----------|-------|-----|------|
| `refresh:{userId}` | refreshToken | 14일 | JWT RefreshToken 저장 |
| `session:qr:{sessionToken}` | placeId | 1시간 | QR 접속 임시 세션 |

---

## 🔑 인덱스 정리

| 테이블 | 인덱스 | 목적 |
|--------|--------|------|
| `sensorLogs` | `(deviceId, createdAt DESC)` | 기기별 최신 로그 조회 |
| `feedback` | `(deviceId, status)` | 기기별 상태 필터 조회 |
| `place` | `qrCode UNIQUE` | QR 코드 중복 방지 및 빠른 조회 |
