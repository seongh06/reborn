// ReBorn — Arduino 온습도 수집 펌웨어
// 보드: Geekble 나노 ESP32-S3
// 센서: DHT22 (SEN0137, DFRobot Gravity 3핀 — VCC/GND/Signal)
//
// 배선:
//   DHT22 VCC    -> 보드 3V3
//   DHT22 GND    -> 보드 GND
//   DHT22 Signal -> 보드 DHTPIN (아래 정의, 보드 실크스크린 라벨과 대조해서 맞는 GPIO로 조정)
//
// 라이브러리 설치 (Arduino IDE: 스케치 > 라이브러리 포함하기 > 라이브러리 관리):
//   - "DHT sensor library" by Adafruit
//   - "Adafruit Unified Sensor" (위 라이브러리의 의존성, 같이 설치됨)
//
// 서버 등록 선행 필요: 이 스케치의 DEVICE_ID가 서버 device 테이블에 미리 등록돼 있어야
// POST 요청이 성공합니다. 관리자 앱 설정 > 장소 > "아두이노 추가"에서 같은 deviceId로 등록하세요.

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <DHT.h>

// ===== 설정값 — 실제 환경에 맞게 수정 =====
const char *WIFI_SSID = "YOUR_WIFI_SSID";
const char *WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

const char *SERVER_HOST = "https://www.reborn-energy.com";
const char *DEVICE_ID = "arduino_test_01"; // 서버에 등록된 deviceKey와 반드시 일치해야 함

#define DHTPIN 4 // DHT22 Signal 핀 — Geekble 보드 실크스크린 확인 후 조정
#define DHTTYPE DHT22

const unsigned long SEND_INTERVAL_MS = 60UL * 1000UL; // 60초마다 전송

// ===== 전역 상태 =====
DHT dht(DHTPIN, DHTTYPE);
unsigned long lastSendAt = 0;

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  Serial.print("WiFi 연결 중");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("WiFi 연결됨, IP: ");
  Serial.println(WiFi.localIP());
}

void sendMetric(float temperature, float humidity) {
  connectWiFi();

  WiFiClientSecure client;
  client.setInsecure(); // Cloudflare 인증서 검증 생략(간단 연결용) — 추후 루트 CA 핀닝 고려 가능

  HTTPClient http;
  String url = String(SERVER_HOST) + "/api/metric/collect";

  if (!http.begin(client, url)) {
    Serial.println("HTTP 연결 시작 실패");
    return;
  }

  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Id", DEVICE_ID);

  char body[128];
  snprintf(body, sizeof(body), "{\"temperature\":%.2f,\"humidity\":%.2f}", temperature, humidity);

  int statusCode = http.POST(body);
  String response = http.getString();

  Serial.print("응답 코드: ");
  Serial.println(statusCode);
  Serial.print("응답 본문: ");
  Serial.println(response);

  http.end();
}

void readAndSend() {
  float humidity = dht.readHumidity();
  float temperature = dht.readTemperature();

  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("DHT22 읽기 실패 — 배선/전원 확인 필요");
    return;
  }

  Serial.print("온도: ");
  Serial.print(temperature);
  Serial.print("°C, 습도: ");
  Serial.print(humidity);
  Serial.println("%");

  sendMetric(temperature, humidity);
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  dht.begin();
  connectWiFi();

  readAndSend(); // 부팅 직후 1회 즉시 전송(대기 없이 바로 확인 가능하도록)
  lastSendAt = millis();
}

void loop() {
  if (millis() - lastSendAt >= SEND_INTERVAL_MS) {
    lastSendAt = millis();
    readAndSend();
  }
}
