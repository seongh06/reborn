#include <DHT.h>
#include <DHT_U.h>

// ReBorn — Arduino Nano 33 IoT 온습도 수집 펌웨어 (WiFiNINA)
// 보드: Arduino Nano 33 IoT (Tools > Board > Arduino SAMD Boards > Arduino Nano 33 IoT)
// 센서: DHT22 (SEN0137, DFRobot Gravity 3핀 — VCC/GND/Signal)
//
// 배선:
//   DHT22 VCC    -> 보드 3V3
//   DHT22 GND    -> 보드 GND
//   DHT22 Signal -> 보드 D2
//
// 라이브러리 설치 (스케치 > 라이브러리 포함하기 > 라이브러리 관리):
//   - "DHT sensor library" by Adafruit (+ 의존성 "Adafruit Unified Sensor") — 원래 쓰던 "DHTNEW"(Rob
//     Tillaart)는 read() 내부 대기 루프에서 인터럽트를 끈 채 응답을 기다리다 타이밍이 안 맞으면 영원히
//     멈추고, Nano 33 IoT(SAMD21 네이티브 USB)에서는 그 여파로 USB(Serial/업로드 포트)까지 완전히 먹통이
//     되는 문제가 있어 교체함. Adafruit 라이브러리는 타임아웃이 있어 실패 시 정상적으로 에러를 반환하고
//     빠져나옴. 상세 트러블슈팅은 Obsidian Reload 리포트 참고.
//   - "WiFiNINA" by Arduino (Nano 33 IoT 보드 패키지 설치 시 보통 같이 설치됨, 없으면 따로 설치)
//   - "ArduinoHttpClient" by Arduino
//
// HTTPS 인증서 등록 필요 (최초 1회, 업로드 전에):
//   도구 > "Upload SSL Root Certificates" 실행 → 목록에 "www.reborn-energy.com" 추가 후 업로드.
//   WiFiNINA는 ESP32의 setInsecure() 같은 검증 생략 옵션이 없어서, NINA 모듈에 루트 인증서를
//   미리 올려둬야 HTTPS 연결이 됩니다. 이 과정 없이 업로드하면 서버 연결에서 계속 실패합니다.
//
// 서버 등록 선행 필요(#147): DEVICE_ID는 실물에 부착된 8자리 시리얼 번호이며, 관리자 앱에서
// 그 시리얼로 기기를 등록해야 POST가 성공합니다.
//
// WiFi는 2.4GHz만 지원 — SSID가 "_5G"로 끝나는 5GHz 전용 네트워크는 연결 안 됨(같은 공유기의
// 2.4GHz용 SSID를 따로 찾아서 사용).
//
// (참고: Geekble 나노 ESP32-S3로 먼저 시도했으나 USB-Serial/JTAG 관련 문제로 시리얼 출력이 전혀
// 안 나오는 하드웨어 결함으로 추정되는 이슈가 있어 Nano 33 IoT로 교체함. 상세 트러블슈팅은
// Obsidian Reload 리포트 참고.)

#include <WiFiNINA.h>
#include <ArduinoHttpClient.h>
#include <DHT.h>

// ===== 설정값 — 실제 환경에 맞게 수정 =====
const char *WIFI_SSID = "SK_WiFiGIGA7000_2.4G";
const char *WIFI_PASSWORD = "JWU52@2119";

const char *SERVER_HOST = "www.reborn-energy.com";
const int SERVER_PORT = 443;
const char *DEVICE_ID = "arduino_test_01"; // 실물에 부착된 시리얼 번호와 반드시 일치해야 함

#define DHTPIN 2 // DHT22 Signal 핀 — Nano 33 IoT의 D2 (특별 기능 없는 안전한 범용 핀)
#define DHTTYPE DHT22

const unsigned long SEND_INTERVAL_MS = 60UL * 1000UL; // 60초마다 전송

// ===== 전역 상태 =====
DHT dht(DHTPIN, DHTTYPE);
WiFiSSLClient wifiClient;
HttpClient httpClient(wifiClient, SERVER_HOST, SERVER_PORT);
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

  char body[128];
  snprintf(body, sizeof(body), "{\"temperature\":%.2f,\"humidity\":%.2f}", temperature, humidity);

  httpClient.beginRequest();
  httpClient.post("/api/metric/collect");
  httpClient.sendHeader("Content-Type", "application/json");
  httpClient.sendHeader("X-Device-Id", DEVICE_ID);
  httpClient.sendHeader("Content-Length", (int)strlen(body));
  httpClient.beginBody();
  httpClient.print(body);
  httpClient.endRequest();

  int statusCode = httpClient.responseStatusCode();
  String response = httpClient.responseBody();

  Serial.print("응답 코드: ");
  Serial.println(statusCode);
  Serial.print("응답 본문: ");
  Serial.println(response);
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
