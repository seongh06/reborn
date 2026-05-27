# 📋 추가 API 명세

---

## 📍 26 로그아웃 API

- **설명:** 인증된 사용자의 로그아웃을 처리하는 API입니다. Redis에 저장된 RefreshToken을 삭제하여 토큰 재사용을 방지합니다.
- **Method:** `POST`
- **Path:** `/api/auth/logout`
- **헤더:** `Authorization: Bearer {accessToken}`

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| refreshToken | 필수 | String | 삭제할 RefreshToken |

**Request Sample**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 로그아웃 완료 및 RefreshToken 삭제 |
| 실패 | 401 | 유효하지 않거나 만료된 AccessToken |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "정상적으로 로그아웃되었습니다.",
  "data": null
}
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 27 장소 목록 조회 API

- **설명:** 인증된 사용자가 속한 모든 장소 목록을 조회하는 API입니다. 사용자의 권한(ADMIN / USER)과 함께 반환되며, 앱 진입 시 장소 선택 화면에서 사용됩니다.
- **Method:** `GET`
- **Path:** `/api/place`
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Sample**

```
GET /api/place
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 장소 목록 조회 성공 |
| 실패 | 401 | 인증 실패 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| places | 필수 | Array | 장소 목록 |
| places[].placeId | 필수 | Int | 장소 고유 ID |
| places[].name | 필수 | String | 장소 이름 |
| places[].type | 필수 | String | 장소 유형 (HOME / STORE / COMPANY) |
| places[].accessLevel | 필수 | String | 내 권한 (ADMIN / USER) |
| places[].createdAt | 필수 | DateTime | 장소 등록일시 (ISO 8601) |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "API 호출 성공",
  "data": {
    "places": [
      {
        "placeId": 501,
        "name": "우리집",
        "type": "HOME",
        "accessLevel": "ADMIN",
        "createdAt": "2026-05-01T10:00:00Z"
      },
      {
        "placeId": 502,
        "name": "강남 스터디카페",
        "type": "STORE",
        "accessLevel": "USER",
        "createdAt": "2026-05-10T09:00:00Z"
      }
    ]
  }
}
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 28 장소 상세 조회 API

- **설명:** 특정 장소의 상세 정보를 조회하는 API입니다. 장소 이름, 유형, 등록된 기기 수, QR 코드 정보를 함께 반환하며, 관리자 앱 대시보드 진입 시 사용됩니다.
- **Method:** `GET`
- **Path:** `/api/place/{placeId}`
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| placeId | 필수 | Int | 조회할 장소 ID (Path Variable) |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Sample**

```
GET /api/place/501
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 장소 상세 조회 성공 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | 해당 장소 접근 권한 없음 |
| 실패 | 404 | 존재하지 않는 장소 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| placeId | 필수 | Int | 장소 고유 ID |
| name | 필수 | String | 장소 이름 |
| type | 필수 | String | 장소 유형 |
| accessLevel | 필수 | String | 내 권한 (ADMIN / USER) |
| deviceCount | 필수 | Int | 등록된 기기 수 |
| qrCode | 선택 | String | QR 코드 식별자 (없을 경우 null) |
| createdAt | 필수 | DateTime | 장소 등록일시 (ISO 8601) |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "API 호출 성공",
  "data": {
    "placeId": 501,
    "name": "우리집",
    "type": "HOME",
    "accessLevel": "ADMIN",
    "deviceCount": 3,
    "qrCode": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "createdAt": "2026-05-01T10:00:00Z"
  }
}
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 403 Forbidden**
```json
{ "success": false, "message": "권한이 없습니다.", "data": null }
```

**실패 케이스 - 404 Not Found**
```json
{ "success": false, "message": "존재하지 않는 장소 정보입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 29 장소 삭제 API

- **설명:** 등록된 장소를 삭제하는 API입니다. 삭제 시 해당 장소에 속한 기기, 센서 로그, 피드백, 매핑 정보가 CASCADE로 함께 삭제됩니다. (ADMIN 권한 필요)
- **Method:** `DELETE`
- **Path:** `/api/place/{placeId}`
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| placeId | 필수 | Int | 삭제할 장소 ID (Path Variable) |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Sample**

```
DELETE /api/place/501
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 장소 삭제 완료 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | ADMIN 권한 없음 |
| 실패 | 404 | 존재하지 않는 장소 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "장소가 삭제되었습니다.",
  "data": null
}
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 403 Forbidden**
```json
{ "success": false, "message": "권한이 없습니다.", "data": null }
```

**실패 케이스 - 404 Not Found**
```json
{ "success": false, "message": "존재하지 않는 장소 정보입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 30 기기 삭제 API

- **설명:** 등록된 IoT 기기를 삭제하는 API입니다. 삭제 시 해당 기기의 센서 로그 및 피드백이 CASCADE로 함께 삭제됩니다. (ADMIN 권한 필요)
- **Method:** `DELETE`
- **Path:** `/api/device/{deviceId}`
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| deviceId | 필수 | String | 삭제할 기기 ID (Path Variable) |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Sample**

```
DELETE /api/device/arduino_room_02
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 기기 삭제 완료 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | ADMIN 권한 없음 |
| 실패 | 404 | 존재하지 않는 기기 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "기기가 삭제되었습니다.",
  "data": null
}
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 403 Forbidden**
```json
{ "success": false, "message": "권한이 없습니다.", "data": null }
```

**실패 케이스 - 404 Not Found**
```json
{ "success": false, "message": "존재하지 않는 기기 정보입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 31 기기 이름 수정 API

- **설명:** 등록된 기기의 이름(방 이름)을 수정하는 API입니다. (ADMIN 권한 필요)
- **Method:** `PATCH`
- **Path:** `/api/device/{deviceId}`
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| deviceId | 필수 | String | 수정할 기기 ID (Path Variable) |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| deviceName | 필수 | String | 변경할 기기 이름 |

**Request Sample**

```json
{
  "deviceName": "작은방"
}
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 기기 이름 수정 완료 |
| 실패 | 400 | deviceName 누락 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | ADMIN 권한 없음 |
| 실패 | 404 | 존재하지 않는 기기 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| deviceId | 필수 | String | 기기 고유 ID |
| deviceName | 필수 | String | 수정된 기기 이름 |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "기기 이름이 수정되었습니다.",
  "data": {
    "deviceId": "arduino_room_02",
    "deviceName": "작은방"
  }
}
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 403 Forbidden**
```json
{ "success": false, "message": "권한이 없습니다.", "data": null }
```

**실패 케이스 - 404 Not Found**
```json
{ "success": false, "message": "존재하지 않는 기기 정보입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 32 알람 읽음 처리 API

- **설명:** 특정 알람을 읽음 상태로 변경하는 API입니다. 알람 목록에서 탭 시 또는 상세 진입 시 호출됩니다.
- **Method:** `PATCH`
- **Path:** `/api/alarm/{alarmId}/read`
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| alarmId | 필수 | Long | 읽음 처리할 알람 ID (Path Variable) |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Sample**

```
PATCH /api/alarm/301/read
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 읽음 처리 완료 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | 본인 알람이 아님 |
| 실패 | 404 | 존재하지 않는 알람 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "읽음 처리되었습니다.",
  "data": null
}
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 403 Forbidden**
```json
{ "success": false, "message": "권한이 없습니다.", "data": null }
```

**실패 케이스 - 404 Not Found**
```json
{ "success": false, "message": "존재하지 않는 알람입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 33 피드백 상태 변경 API

- **설명:** 접수된 피드백의 처리 상태를 변경하는 API입니다. 관리자가 피드백을 확인 후 승인(APPROVED) 또는 거절(REJECTED)로 처리합니다. (ADMIN 권한 필요)
- **Method:** `PATCH`
- **Path:** `/api/feedback/{feedbackId}`
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| feedbackId | 필수 | Int | 상태를 변경할 피드백 ID (Path Variable) |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| status | 필수 | String | 변경할 상태 (APPROVED / REJECTED) |

**Request Sample**

```json
{
  "status": "APPROVED"
}
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 피드백 상태 변경 완료 |
| 실패 | 400 | 잘못된 status 값 또는 이미 처리된 피드백 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | ADMIN 권한 없음 |
| 실패 | 404 | 존재하지 않는 피드백 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| feedbackId | 필수 | Int | 피드백 고유 ID |
| status | 필수 | String | 변경된 상태 |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "피드백 상태가 변경되었습니다.",
  "data": {
    "feedbackId": 1024,
    "status": "APPROVED"
  }
}
```

**실패 케이스 - 400 Bad Request**
```json
{ "success": false, "message": "이미 처리된 피드백입니다.", "data": null }
```

**실패 케이스 - 401 Unauthorized**
```json
{ "success": false, "message": "인증 정보가 유효하지 않습니다. 다시 로그인해주세요.", "data": null }
```

**실패 케이스 - 403 Forbidden**
```json
{ "success": false, "message": "권한이 없습니다.", "data": null }
```

**실패 케이스 - 404 Not Found**
```json
{ "success": false, "message": "존재하지 않는 피드백입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 🟡 고도화 시 추가 예정 API (34~40)

| # | API명 | Method | Path | 설명 |
|---|-------|--------|------|------|
| 34 | 내 프로필 조회 | `GET` | `/api/user/me` | 설정 화면에서 이름, 이메일 표시 |
| 35 | 내 프로필 수정 | `PATCH` | `/api/user/me` | 이름 변경 등 |
| 36 | 장소 멤버 목록 조회 | `GET` | `/api/place/{placeId}/members` | 같은 장소 관리자 목록 확인 |
| 37 | 장소 멤버 추방 | `DELETE` | `/api/place/{placeId}/members/{userId}` | 관리자 권한 회수 |
| 38 | 알람 전체 읽음 처리 | `PATCH` | `/api/alarm/read-all` | 알람 일괄 읽음 |
| 39 | 공기계 상태 갱신 | `PATCH` | `/api/device/{deviceId}/status` | 공기계 앱 heartbeat용 |

> 🔲 34~39번은 MVP 완성 후 고도화(Phase 10) 단계에서 구현 예정

---

## 🟢 WebSocket 연결 명세

| # | 연결 | Path | 설명 |
|---|------|------|------|
| WS-01 | 공기계 앱 연결 | `WS /ws/kiosk` | 공기계 앱이 서버에 상시 연결 유지, 제어 명령 수신 |
| WS-02 | 관리자 앱 연결 | `WS /ws/admin` | 제어 명령 송신 및 실시간 센서 데이터 수신 |
