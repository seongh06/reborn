# 🔌 IoT 기기 API 명세

---

## 📍 17 장소별 IoT 기기 목록 조회 API

- **설명:** 특정 장소에 등록된 모든 IoT 기기(Arduino 센서 및 공기계 앱) 목록을 조회하는 API입니다. 기기의 온라인 상태 및 기기 유형을 함께 반환하며, 관리자 앱의 기기 관리 화면에서 사용됩니다. (인증 필요)
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| placeId | 필수 | Int | 조회할 장소의 고유 ID |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Sample**

```
GET /api/device?placeId=501
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 기기 목록 조회 성공 |
| 실패 | 400 | placeId 누락 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | 해당 장소 접근 권한 없음 |
| 실패 | 404 | 존재하지 않는 장소 |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| devices | 필수 | Array | 기기 목록 |
| devices[].deviceId | 필수 | String | 기기 고유 ID |
| devices[].deviceName | 필수 | String | 기기 이름 (방 이름) |
| devices[].deviceType | 필수 | String | 기기 유형 (ARDUINO / KIOSK) |
| devices[].isOnline | 필수 | Boolean | 현재 온라인 여부 |
| devices[].createdAt | 필수 | DateTime | 기기 등록일시 (ISO 8601) |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "API 호출 성공",
  "data": {
    "devices": [
      {
        "deviceId": "arduino_room_01",
        "deviceName": "거실",
        "deviceType": "ARDUINO",
        "isOnline": true,
        "createdAt": "2026-05-01T10:00:00Z"
      },
      {
        "deviceId": "kiosk_a3f7k2_1716",
        "deviceName": "거실 공기계",
        "deviceType": "KIOSK",
        "isOnline": true,
        "createdAt": "2026-05-01T10:05:00Z"
      }
    ]
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

## 📍 18 새로운 IoT 기기 추가 API

- **설명:** 장소에 새로운 Arduino IoT 기기를 등록하는 API입니다. 공기계 앱(KIOSK) 등록은 페어링 코드 방식을 사용하며, 이 API는 Arduino 기기(ARDUINO) 직접 등록에 사용됩니다. (ADMIN 권한 필요)
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| placeId | 필수 | Int | 기기를 등록할 장소 ID |
| deviceId | 필수 | String | Arduino 기기의 고유 ID (펌웨어에 설정된 값) |
| deviceName | 필수 | String | 기기 이름 (예: 안방, 주방) |

**Request Sample**

```json
{
  "placeId": 501,
  "deviceId": "arduino_room_02",
  "deviceName": "안방"
}
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 기기 등록 완료 |
| 실패 | 400 | 필수 필드 누락 |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | ADMIN 권한 없음 |
| 실패 | 404 | 존재하지 않는 장소 |
| 실패 | 409 | 이미 등록된 deviceId |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| deviceId | 필수 | String | 등록된 기기의 고유 ID |
| deviceName | 필수 | String | 기기 이름 |
| deviceType | 필수 | String | 기기 유형 (ARDUINO) |
| createdAt | 필수 | DateTime | 기기 등록일시 (ISO 8601) |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "기기가 정상적으로 등록되었습니다.",
  "data": {
    "deviceId": "arduino_room_02",
    "deviceName": "안방",
    "deviceType": "ARDUINO",
    "createdAt": "2026-05-17T21:05:04Z"
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

**실패 케이스 - 409 Conflict**
```json
{ "success": false, "message": "이미 등록된 기기 ID입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```

---

## 📍 19 IoT 기기 제어 API

- **설명:** 관리자 앱에서 특정 공기계 앱(KIOSK)을 통해 IoT 기기(에어컨 등)를 제어하는 명령을 전달하는 API입니다. 서버는 WebSocket을 통해 해당 공기계 앱으로 제어 명령을 중계하며, 공기계 앱이 Wi-Fi로 IoT 기기를 직접 제어합니다. (ADMIN 권한 필요)
- **헤더:** `Authorization: Bearer {accessToken}`

**Parameter**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |

**Request Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| kioskDeviceId | 필수 | String | 명령을 수신할 공기계 앱의 deviceId |
| command | 필수 | String | 제어 명령 유형 (POWER_ON / POWER_OFF / TEMP_UP / TEMP_DOWN / SET_TEMP) |
| value | 선택 | Int | 명령에 따른 값 (SET_TEMP일 경우 설정 온도, 예: 24) |

**Request Sample**

```json
{
  "kioskDeviceId": "kiosk_a3f7k2_1716",
  "command": "SET_TEMP",
  "value": 24
}
```

**응답코드**

| 성공 여부 | 응답 코드 | 설명 |
| --- | --- | --- |
| 성공 | 200 | 제어 명령 전달 완료 (공기계 앱 수신 대기 중) |
| 실패 | 400 | 필수 필드 누락 또는 정의되지 않은 command |
| 실패 | 401 | 인증 실패 |
| 실패 | 403 | ADMIN 권한 없음 |
| 실패 | 404 | 등록되지 않은 공기계 기기 |
| 실패 | 503 | 공기계 앱 오프라인 (WebSocket 연결 없음) |
| 실패 | 500 | 서버 내부 오류 |

**Response Body**

| 필드 이름 | 필수 여부 | 타입 | 설명 |
| --- | --- | --- | --- |
| kioskDeviceId | 필수 | String | 명령을 수신한 공기계 deviceId |
| command | 필수 | String | 전달된 제어 명령 |
| value | 선택 | Int | 전달된 명령 값 |
| sentAt | 필수 | DateTime | 명령 전달 시간 (ISO 8601) |

**JSON 형식 응답**

```json
{
  "success": true,
  "message": "제어 명령이 전달되었습니다.",
  "data": {
    "kioskDeviceId": "kiosk_a3f7k2_1716",
    "command": "SET_TEMP",
    "value": 24,
    "sentAt": "2026-05-17T21:05:04Z"
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
{ "success": false, "message": "등록되지 않은 기기입니다.", "data": null }
```

**실패 케이스 - 503 Service Unavailable**
```json
{ "success": false, "message": "공기계 앱이 현재 오프라인 상태입니다.", "data": null }
```

**실패 케이스 - 500 Internal Server Error**
```json
{ "success": false, "message": "서버 내부 오류가 발생했습니다.", "data": null }
```
