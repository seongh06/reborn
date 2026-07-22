// ReBorn — AI 스피커(#142) 펌웨어
// 보드: M5Stack ATOM Echo (ESP32-PICO-D4, PSRAM 없음)
//
// 배선은 ATOM Echo 내장 하드웨어 고정값(온보드 마이크/스피커/LED/버튼) 이므로 별도 배선 불필요:
//   I2S 공용 BCK = G19, LRCK(WS) = G33
//   I2S 스피커(NS4168 앰프) DATA OUT = G22
//   I2S 마이크(PDM MEMS)  DATA IN  = G23
//   온보드 버튼 = G39 (active-LOW, 보드 자체에 풀업 있음)
//   온보드 RGB LED(SK6812) = G27, 1구
// (핀 배치는 기존 M5Atom-Echo 예제 관례를 따름 — 실기기로 아직 검증 못했음, 최초 업로드 후 확인 필요)
//
// 라이브러리 설치 (Arduino IDE: 스케치 > 라이브러리 포함하기 > 라이브러리 관리):
//   - "Adafruit NeoPixel" — 온보드 LED 상태 표시용
//   보드 패키지: "esp32 by Espressif Systems" (코어 3.x, IDF 5.x 기반).
//   I2S는 코어 3.x에서 번들 제공되는 "ESP_I2S"(I2SClass, <ESP_I2S.h>)를 사용한다 — 레거시
//   <driver/i2s.h>는 이 코어 버전에 없어서 사용 불가(설치된 esp32 코어 3.3.10 기준 확인).
//
// ⚠️ ATOM Echo의 MCU(ESP32-PICO-D4)는 SOC_I2S_HW_VERSION_1이라, I2SClass가 mono 요청 시
//    내부적으로 stereo(BOTH 슬롯)로 전환하는 하드웨어 workaround를 갖고 있다(RX는 라이브러리가
//    투명하게 mono로 되돌려주지만, TX는 write()에 넘기는 버퍼가 실제로는 stereo(L/R 인터리브)
//    포맷이어야 할 가능성이 있음 — 미검증). 첫 업로드 후 스피커에서 피치가 이상하거나 잡음이 나면
//    이 부분(TX를 STEREO로 명시하고 좌우 동일 샘플을 복제해서 쓰는 방식)부터 의심할 것.
//
// 서버 등록 선행 필요: DEVICE_ID가 서버 device 테이블에 deviceType=AI_SPEAKER로 미리 등록돼 있어야
// POST /api/feedback/voice 가 성공합니다.
//
// ⚠️ 중요한 설계 제약 (PSRAM 없음):
//   오디오를 통째로 버퍼링하면 안 되고, I2S에서 읽은 작은 청크(RECORD_CHUNK_BYTES)를 즉시
//   HTTP chunked transfer-encoding으로 흘려보낸다. 응답으로 받는 TTS 오디오도 동일하게
//   Content-Length만큼 소켓에서 조금씩 읽어 즉시 I2S로 재생하고 버리는 스트리밍 방식이다.
//   녹음 WAV 헤더는 최종 길이를 미리 알 수 없어 RIFF/data 청크 크기를 0xFFFFFFFF(스트리밍
//   placeholder)로 채운다 — 대부분의 디코더는 크기 필드보다 실제 전송 바이트(HTTP 프레이밍)를
//   우선하지만, Gemini가 이를 올바르게 처리하는지는 실제 키로 curl 검증 전까지는 가정임.

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <WebServer.h>
#include <DNSServer.h>
#include <Preferences.h>
#include <Adafruit_NeoPixel.h>
#include <ESP_I2S.h>
#include "voice_response_meta.h"

// ===== 설정값 =====
// WiFi SSID/PW와 기기 ID는 더 이상 하드코딩하지 않는다(#143). 최초 부팅 시(또는 버튼을 꾹 눌러
// 리셋 시) NVS에 저장된 값이 없으면 SoftAP 프로비저닝 포털을 띄워 휴대폰으로 설정을 받는다.
// 관련 함수: loadProvisioning(), runProvisioningPortal() — 아래 "SoftAP 프로비저닝(#143)" 섹션.
const char *SERVER_HOST = "www.reborn-energy.com";
const uint16_t SERVER_PORT = 443;

String g_wifiSsid;
String g_wifiPassword;
String g_deviceId; // 서버에 등록된 deviceKey와 반드시 일치해야 함(휴대폰 설정 화면에서 입력)

// ===== 핀 정의 (ATOM Echo 고정) =====
#define I2S_BCK_PIN       19
#define I2S_LRCK_PIN      33
#define I2S_DATA_OUT_PIN  22 // 스피커
#define I2S_DATA_IN_PIN   23 // 마이크
#define BUTTON_PIN        39
#define LED_PIN           27

#define MIC_SAMPLE_RATE 16000
#define MIC_BITS_PER_SAMPLE 16
#define DEFAULT_PLAYBACK_SAMPLE_RATE 24000 // Gemini TTS 기본 응답 레이트(audio/L16;rate=24000) 가정값

// 마이크 읽기/스피커 쓰기용 스트리밍 청크 크기(바이트) — 절대 이보다 큰 통버퍼를 만들지 말 것
#define STREAM_CHUNK_BYTES 1024

// ===== 버튼/재시도 상태머신 타이밍 =====
const unsigned long BASE_RECORD_MS = 10000UL;  // 최초 녹음 창(10초)
const unsigned long RETRY_EXTRA_MS = 5000UL;   // 재시도마다 녹음 창 +5초
const unsigned long RETRY_WINDOW_MS = 30000UL; // 인식 실패 후 재시도 허용 창(30초, 지나면 조용히 리셋)
const unsigned long BUTTON_DEBOUNCE_MS = 50UL;

// ===== 전역 상태 =====
Adafruit_NeoPixel pixel(1, LED_PIN, NEO_GRB + NEO_KHZ800);
I2SClass i2s;

enum class SpeakerState {
  IDLE,        // 버튼 대기
  RECORDING,   // 녹음 + 스트리밍 업로드 중
  RETRY_WAIT,  // 인식 실패 후 재시도 대기(30초 창)
};

SpeakerState state = SpeakerState::IDLE;
unsigned long currentRecordWindowMs = BASE_RECORD_MS;
unsigned long retryDeadlineAt = 0;

bool lastButtonRaw = HIGH; // active-LOW, 평상시 HIGH
bool lastButtonStable = HIGH;
unsigned long lastButtonChangeAt = 0;

static uint8_t streamBuf[STREAM_CHUNK_BYTES];

// ===== SoftAP 프로비저닝(#143) =====
// WiFi SSID/PW·기기 ID를 NVS(Preferences)에 저장해두고, 없으면 기기가 자체적으로 AP(SoftAP)를
// 열어 휴대폰이 그 AP에 접속해 웹 폼으로 값을 입력하게 한다. Nano33 IoT(WiFiNINA) 쪽은 WiFi
// 스택이 달라 이 구현을 그대로 재사용할 수 없음 — 같은 패턴(설정 없으면 AP+웹폼, 저장 후 재부팅)을
// WiFiNINA API로 별도 이식해야 함(다음 세션 TODO, Obsidian 이슈노트 참고).
//
// CodeRabbit 리뷰(PR #144) 지적 반영: 프로비저닝 AP 비밀번호를 모든 기기 공통 고정값으로 두면
// 설정 대기 상태(부팅 3초 홀드로 언제든 재진입 가능)인 임의의 기기 근처에서 그 값을 알고 있는
// 공격자가 접속해 WiFi/기기 ID를 재기록할 수 있다 — 기기별 MAC 기반 고유 비밀번호로 변경.
String buildDeviceApPassword() {
  uint64_t mac = ESP.getEfuseMac();
  char buf[15];
  snprintf(buf, sizeof(buf), "rb-%012llx", (unsigned long long)mac); // "rb-" + MAC 12자리 = 15자(WPA2 8자 이상 충족)
  return String(buf);
}

bool loadProvisioning() {
  Preferences prefs;
  prefs.begin("reborn", true);
  g_wifiSsid = prefs.getString("ssid", "");
  g_wifiPassword = prefs.getString("pass", "");
  g_deviceId = prefs.getString("device_id", "");
  prefs.end();
  return g_wifiSsid.length() > 0 && g_deviceId.length() > 0;
}

void saveProvisioning(const String &ssid, const String &password, const String &deviceId) {
  Preferences prefs;
  prefs.begin("reborn", false);
  prefs.putString("ssid", ssid);
  prefs.putString("pass", password);
  prefs.putString("device_id", deviceId);
  prefs.end();
}

void clearProvisioning() {
  Preferences prefs;
  prefs.begin("reborn", false);
  prefs.clear();
  prefs.end();
}

// 재프로비저닝 트리거: 부팅 시 버튼을 3초 이상 누르고 있으면 저장된 설정을 지우고 포털을 연다.
bool isResetHeldAtBoot() {
  if (digitalRead(BUTTON_PIN) != LOW) return false; // active-LOW
  unsigned long heldSince = millis();
  while (digitalRead(BUTTON_PIN) == LOW) {
    if (millis() - heldSince > 3000UL) return true;
    delay(20);
  }
  return false;
}

String buildProvisioningFormHtml() {
  return String(
    "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
    "<title>ReBorn 기기 설정</title></head>"
    "<body style=\"font-family:sans-serif;padding:20px;\">"
    "<h2>ReBorn AI 스피커 설정</h2>"
    "<form action=\"/save\" method=\"POST\">"
    "WiFi SSID (2.4GHz만 지원)<br><input type=\"text\" name=\"ssid\" required><br><br>"
    "WiFi 비밀번호<br><input type=\"password\" name=\"password\"><br><br>"
    "기기 ID (관리자 앱에 등록한 deviceKey)<br><input type=\"text\" name=\"device_id\" required><br><br>"
    "<button type=\"submit\">저장하고 재부팅</button>"
    "</form></body></html>"
  );
}

// 블로킹 — 설정이 저장되어 재부팅될 때까지 리턴하지 않는다.
void runProvisioningPortal() {
  setLed(20, 0, 20); // 보라 계열 = 설정 대기

  String apSsid = "ReBorn-Setup-" + String((uint32_t)(ESP.getEfuseMac() & 0xFFFFUL), HEX);
  String apPassword = buildDeviceApPassword();
  WiFi.mode(WIFI_AP);
  WiFi.softAP(apSsid.c_str(), apPassword.c_str());
  IPAddress apIp = WiFi.softAPIP();

  DNSServer dns;
  dns.start(53, "*", apIp); // 캡티브 포털 — 모든 DNS 질의를 AP IP로 응답

  WebServer server(80);
  server.on("/", HTTP_GET, [&server]() {
    server.send(200, "text/html", buildProvisioningFormHtml());
  });
  server.on("/save", HTTP_POST, [&server]() {
    String ssid = server.arg("ssid");
    String password = server.arg("password");
    String deviceId = server.arg("device_id");
    saveProvisioning(ssid, password, deviceId);
    server.send(200, "text/html", "<html><body>저장 완료 — 기기가 재부팅됩니다.</body></html>");
    delay(1000);
    ESP.restart();
  });
  server.onNotFound([&server, apIp]() {
    server.sendHeader("Location", "http://" + apIp.toString() + "/", true);
    server.send(302, "text/plain", "");
  });
  server.begin();

  Serial.printf("프로비저닝 포털 시작 — SSID: %s (PW: %s), http://%s\n", apSsid.c_str(), apPassword.c_str(), apIp.toString().c_str());

  while (true) {
    dns.processNextRequest();
    server.handleClient();
    delay(2);
  }
}

// ===== LED =====
void setLed(uint8_t r, uint8_t g, uint8_t b) {
  pixel.setPixelColor(0, pixel.Color(r, g, b));
  pixel.show();
}

// ===== WiFi =====
// CodeRabbit 리뷰 지적 반영: 기존엔 WL_CONNECTED가 될 때까지 무한 대기해서, 자격 증명이
// 잘못됐거나 AP가 사라지면 기기가 영원히 멈췄다(재부팅+3초 홀드로만 복구 가능) — 타임아웃을
// 두고 실패 시 false를 반환해 호출부가 재시도/프로비저닝 포털 폴백을 결정하게 한다.
bool connectWiFi(unsigned long timeoutMs) {
  if (WiFi.status() == WL_CONNECTED) return true;

  Serial.print("WiFi 연결 중");
  WiFi.begin(g_wifiSsid.c_str(), g_wifiPassword.c_str());
  setLed(20, 0, 20); // 보라 = WiFi 연결 중
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - start > timeoutMs) {
      Serial.println();
      Serial.println("WiFi 연결 타임아웃");
      return false;
    }
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("WiFi 연결됨, IP: ");
  Serial.println(WiFi.localIP());
  return true;
}

// ===== I2S =====
// setPins(bclk, ws, dout, din) 두 핀(dout/din)이 모두 지정되면 I2SClass가 TX/RX 채널을
// 하나의 i2s_new_channel 호출로 함께 만든다(ATOM Echo는 마이크/스피커가 BCK·WS를 공유하는
// 풀듀플렉스 배선이라 이 방식이 맞음). 이후 configureRX()/configureTX()로 녹음↔재생 전환 시
// 샘플레이트만 바꿔 쓴다 — 매번 통버퍼를 새로 만들지 않는다.
void i2sInstall() {
  i2s.setPins(I2S_BCK_PIN, I2S_LRCK_PIN, I2S_DATA_OUT_PIN, I2S_DATA_IN_PIN);
  bool ok = i2s.begin(I2S_MODE_STD, MIC_SAMPLE_RATE, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);
  if (!ok) {
    Serial.println("I2S 초기화 실패");
  }
}

void i2sSetRxRate(uint32_t rate) {
  i2s.configureRX(rate, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);
}

void i2sSetTxRate(uint32_t rate) {
  i2s.configureTX(rate, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);
}

// ===== WAV 헤더(스트리밍용 placeholder 크기) =====
// RIFF/data 청크 크기를 알 수 없으므로 0xFFFFFFFF로 채운다. Gemini가 이를 받아들이는지는
// 미검증 — 다음 세션에서 curl로 실제 응답 검증 필요(devlog 메모 참고).
size_t buildWavHeader(uint8_t *out, uint32_t sampleRate, uint16_t bitsPerSample, uint16_t numChannels) {
  uint32_t byteRate = sampleRate * numChannels * (bitsPerSample / 8);
  uint16_t blockAlign = numChannels * (bitsPerSample / 8);

  memcpy(out + 0, "RIFF", 4);
  uint32_t riffSize = 0xFFFFFFFF;
  memcpy(out + 4, &riffSize, 4);
  memcpy(out + 8, "WAVE", 4);
  memcpy(out + 12, "fmt ", 4);
  uint32_t fmtChunkSize = 16;
  memcpy(out + 16, &fmtChunkSize, 4);
  uint16_t audioFormat = 1; // PCM
  memcpy(out + 20, &audioFormat, 2);
  memcpy(out + 22, &numChannels, 2);
  memcpy(out + 24, &sampleRate, 4);
  memcpy(out + 28, &byteRate, 4);
  memcpy(out + 32, &blockAlign, 2);
  memcpy(out + 34, &bitsPerSample, 2);
  memcpy(out + 36, "data", 4);
  uint32_t dataSize = 0xFFFFFFFF;
  memcpy(out + 40, &dataSize, 4);

  return 44;
}

// ===== HTTP chunked 전송 helper =====
void writeChunk(WiFiClientSecure &client, const uint8_t *data, size_t len) {
  if (len == 0) return;
  client.printf("%X\r\n", (unsigned int)len);
  client.write(data, len);
  client.print("\r\n");
}

void writeFinalChunk(WiFiClientSecure &client) {
  client.print("0\r\n\r\n");
}

// 응답 Content-Type 헤더(예: "audio/L16;rate=24000")에서 rate 파라미터를 파싱.
// 못 찾으면 DEFAULT_PLAYBACK_SAMPLE_RATE 사용.
uint32_t parseSampleRate(const String &contentType) {
  int idx = contentType.indexOf("rate=");
  if (idx < 0) return DEFAULT_PLAYBACK_SAMPLE_RATE;
  int start = idx + 5;
  int end = start;
  while (end < (int)contentType.length() && isDigit(contentType[end])) end++;
  if (end == start) return DEFAULT_PLAYBACK_SAMPLE_RATE;
  return (uint32_t)contentType.substring(start, end).toInt();
}

// 상태줄 + 헤더를 한 줄씩 읽어 파싱. 빈 줄(헤더 끝) 이후 본문 스트리밍은 호출부에서 처리.
VoiceResponseMeta readResponseHeaders(WiFiClientSecure &client) {
  VoiceResponseMeta meta;

  unsigned long deadline = millis() + 15000UL;
  String statusLine = "";
  while (client.connected() && millis() < deadline) {
    if (client.available()) {
      statusLine = client.readStringUntil('\n');
      break;
    }
    delay(5);
  }
  if (statusLine.indexOf(" 200 ") < 0) {
    Serial.print("서버 응답 실패(상태줄): ");
    Serial.println(statusLine);
    return meta;
  }

  String contentType = "";
  bool headersEnded = false;
  unsigned long lastDataAt = millis(); // streamPlayback과 동일한 5초 정체 타임아웃 패턴
  while (client.connected() && millis() - lastDataAt < 5000UL) {
    if (!client.available()) {
      delay(5);
      continue;
    }
    lastDataAt = millis();
    String line = client.readStringUntil('\n');
    line.trim();
    if (line.length() == 0) {
      headersEnded = true;
      break; // 헤더 끝
    }

    String lower = line;
    lower.toLowerCase();
    if (lower.startsWith("content-length:")) {
      meta.contentLength = line.substring(line.indexOf(':') + 1).toInt();
    } else if (lower.startsWith("content-type:")) {
      contentType = line.substring(line.indexOf(':') + 1);
      contentType.trim();
    } else if (lower.startsWith("x-feedback-recognized:")) {
      String v = line.substring(line.indexOf(':') + 1);
      v.trim();
      meta.recognized = v.equalsIgnoreCase("true");
    }
  }
  if (!headersEnded) {
    Serial.println("응답 헤더 수신 중 타임아웃/연결 끊김");
    return meta; // meta.ok는 false로 유지된 채 반환
  }

  meta.sampleRate = parseSampleRate(contentType);
  meta.ok = true;
  return meta;
}

// 응답 본문(Content-Length 바이트)을 조금씩 읽어 즉시 I2S로 재생 — 절대 통버퍼링하지 않음.
void streamPlayback(WiFiClientSecure &client, long contentLength) {
  if (contentLength <= 0) return;

  long remaining = contentLength;
  size_t bytesWritten;
  unsigned long lastDataAt = millis();

  while (remaining > 0 && client.connected() && millis() - lastDataAt < 5000UL) {
    size_t want = (size_t)min((long)STREAM_CHUNK_BYTES, remaining);
    int n = client.read(streamBuf, want);
    if (n <= 0) {
      delay(5);
      continue;
    }
    lastDataAt = millis();
    bytesWritten = i2s.write(streamBuf, (size_t)n);
    (void)bytesWritten;
    remaining -= n;
  }
}

// CodeRabbit 리뷰 지적 반영: setInsecure()는 인증서 검증을 생략해 MITM이 업로드되는 음성이나
// 응답 TTS·기기 식별자를 가로채거나 조작할 수 있다(reborn_dht22.ino와 같은 정책이었으나, 그쪽은
// 온습도 push뿐이라 상대적으로 영향이 작고 이쪽은 사용자 음성이 실려서 더 민감). www.reborn-energy.com이
// 실제 사용하는 체인의 루트(Google Trust Services "GTS Root R4", 2026-07-21 openssl s_client로 확인,
// 유효기간 ~2028-01-28)를 핀닝한다. 서버가 리프+중간(WE1) 인증서를 함께 보내주므로 루트만 신뢰하면
// mbedTLS가 전체 체인을 검증할 수 있다. ⚠️ 루트 인증서가 교체되면(자주 있는 일은 아님) 이 상수도
// 갱신해야 함 — 그때까지는 기기가 서버에 연결하지 못하게 된다.
const char *SERVER_ROOT_CA_PEM = R"CERT(
-----BEGIN CERTIFICATE-----
MIIDejCCAmKgAwIBAgIQf+UwvzMTQ77dghYQST2KGzANBgkqhkiG9w0BAQsFADBX
MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQR2xvYmFsU2lnbiBudi1zYTEQMA4GA1UE
CxMHUm9vdCBDQTEbMBkGA1UEAxMSR2xvYmFsU2lnbiBSb290IENBMB4XDTIzMTEx
NTAzNDMyMVoXDTI4MDEyODAwMDA0MlowRzELMAkGA1UEBhMCVVMxIjAgBgNVBAoT
GUdvb2dsZSBUcnVzdCBTZXJ2aWNlcyBMTEMxFDASBgNVBAMTC0dUUyBSb290IFI0
MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE83Rzp2iLYK5DuDXFgTB7S0md+8Fhzube
Rr1r1WEYNa5A3XP3iZEwWus87oV8okB2O6nGuEfYKueSkWpz6bFyOZ8pn6KY019e
WIZlD6GEZQbR3IvJx3PIjGov5cSr0R2Ko4H/MIH8MA4GA1UdDwEB/wQEAwIBhjAd
BgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDwYDVR0TAQH/BAUwAwEB/zAd
BgNVHQ4EFgQUgEzW63T/STaj1dj8tT7FavCUHYwwHwYDVR0jBBgwFoAUYHtmGkUN
l8qJUC99BM00qP/8/UswNgYIKwYBBQUHAQEEKjAoMCYGCCsGAQUFBzAChhpodHRw
Oi8vaS5wa2kuZ29vZy9nc3IxLmNydDAtBgNVHR8EJjAkMCKgIKAehhxodHRwOi8v
Yy5wa2kuZ29vZy9yL2dzcjEuY3JsMBMGA1UdIAQMMAowCAYGZ4EMAQIBMA0GCSqG
SIb3DQEBCwUAA4IBAQAYQrsPBtYDh5bjP2OBDwmkoWhIDDkic574y04tfzHpn+cJ
odI2D4SseesQ6bDrarZ7C30ddLibZatoKiws3UL9xnELz4ct92vID24FfVbiI1hY
+SW6FoVHkNeWIP0GCbaM4C6uVdF5dTUsMVs/ZbzNnIdCp5Gxmx5ejvEau8otR/Cs
kGN+hr/W5GvT1tMBjgWKZ1i4//emhA1JG1BbPzoLJQvyEotc03lXjTaCzv8mEbep
8RqZ7a2CPsgRbuvTPBwcOMBBmuFeU88+FSBX6+7iP0il8b4Z0QFqIwwMHfs/L6K1
vepuoxtGzi4CZ68zJpiq1UvSqTbFJjtbD4seiMHl
-----END CERTIFICATE-----
)CERT";

// ===== 녹음 → 스트리밍 업로드 → 응답 재생 (한 사이클) =====
// 반환값: 인식 성공 여부. 네트워크 오류 시 false.
bool recordUploadAndPlay(unsigned long recordWindowMs) {
  WiFiClientSecure client;
  client.setCACert(SERVER_ROOT_CA_PEM);

  setLed(0, 0, 40); // 파랑 = 녹음 중
  Serial.println("녹음 시작");

  if (!client.connect(SERVER_HOST, SERVER_PORT)) {
    Serial.println("서버 연결 실패");
    setLed(40, 0, 0);
    delay(500);
    return false;
  }

  client.print("POST /api/feedback/voice HTTP/1.1\r\n");
  client.printf("Host: %s\r\n", SERVER_HOST);
  client.printf("X-Device-Id: %s\r\n", g_deviceId.c_str());
  client.print("Content-Type: audio/wav\r\n");
  client.print("Transfer-Encoding: chunked\r\n");
  client.print("Connection: close\r\n\r\n");

  i2sSetRxRate(MIC_SAMPLE_RATE);

  uint8_t wavHeader[44];
  size_t headerLen = buildWavHeader(wavHeader, MIC_SAMPLE_RATE, MIC_BITS_PER_SAMPLE, 1);
  writeChunk(client, wavHeader, headerLen);

  unsigned long recordStart = millis();
  bool stoppedEarly = false;
  while (millis() - recordStart < recordWindowMs) {
    if (isButtonJustPressed()) {
      stoppedEarly = true;
      break;
    }

    size_t bytesRead = i2s.readBytes((char *)streamBuf, STREAM_CHUNK_BYTES);
    if (bytesRead > 0) {
      writeChunk(client, streamBuf, bytesRead);
    }
  }
  (void)stoppedEarly;

  writeFinalChunk(client);
  Serial.println("업로드 완료, 응답 대기 중");

  setLed(40, 40, 0); // 노랑 = 분석 대기

  VoiceResponseMeta meta = readResponseHeaders(client);
  if (!meta.ok) {
    Serial.println("서버 응답 처리 실패 - 헤더 파싱 실패 또는 타임아웃");
    client.stop();
    setLed(40, 0, 0);
    delay(500);
    return false;
  }

  Serial.printf("서버 응답 수신 - 인식 결과: %s\n", meta.recognized ? "성공" : "실패(재시도 필요)");

  i2sSetTxRate(meta.sampleRate);
  setLed(meta.recognized ? 0 : 40, meta.recognized ? 40 : 20, 0); // 초록=성공, 주황=재시도 안내
  streamPlayback(client, meta.contentLength);

  client.stop();
  return meta.recognized;
}

// ===== 버튼 =====
// 디바운스된 "새로 눌림" 이벤트 1회만 반환(호출할 때마다 최신 상태 반영).
bool isButtonJustPressed() {
  bool raw = digitalRead(BUTTON_PIN); // active-LOW
  unsigned long now = millis();

  if (raw != lastButtonRaw) {
    lastButtonRaw = raw;
    lastButtonChangeAt = now;
  }

  bool justPressed = false;
  if (now - lastButtonChangeAt > BUTTON_DEBOUNCE_MS && lastButtonStable != raw) {
    lastButtonStable = raw;
    if (lastButtonStable == LOW) {
      justPressed = true;
    }
  }
  return justPressed;
}

// ===== setup / loop =====
void setup() {
  Serial.begin(115200);
  delay(300);

  pinMode(BUTTON_PIN, INPUT);
  pixel.begin();
  setLed(0, 0, 0);

  if (isResetHeldAtBoot()) {
    Serial.println("버튼 3초 이상 감지 — 저장된 설정 초기화 후 프로비저닝 포털 진입");
    clearProvisioning();
  }

  if (!loadProvisioning()) {
    Serial.println("저장된 WiFi/기기 설정 없음 — 프로비저닝 포털 시작");
    runProvisioningPortal(); // 저장 완료 시 내부에서 재부팅되어 반환하지 않음
  }

  i2sInstall();
  if (!connectWiFi(20000UL)) {
    Serial.println("WiFi 연결 실패 — 저장된 자격 증명이 잘못됐을 수 있어 프로비저닝 포털로 폴백");
    runProvisioningPortal(); // 저장 완료 시 내부에서 재부팅되어 반환하지 않음
  }

  setLed(0, 0, 0); // 대기 = LED 꺼짐
  Serial.println("대기 중 — 버튼을 눌러 피드백을 남겨주세요");
}

void loop() {
  connectWiFi(5000UL); // 끊겼으면 재연결 시도(최대 5초) — 실패해도 다음 loop()에서 다시 시도

  switch (state) {
    case SpeakerState::IDLE: {
      if (isButtonJustPressed()) {
        Serial.println("버튼 눌림 감지 - 녹음 시작");
        currentRecordWindowMs = BASE_RECORD_MS;
        bool recognized = recordUploadAndPlay(currentRecordWindowMs);
        if (recognized) {
          state = SpeakerState::IDLE;
          setLed(0, 0, 0);
        } else {
          state = SpeakerState::RETRY_WAIT;
          retryDeadlineAt = millis() + RETRY_WINDOW_MS;
        }
      }
      break;
    }

    case SpeakerState::RETRY_WAIT: {
      if (millis() > retryDeadlineAt) {
        // 30초 안에 재시도 없으면 조용히 리셋(추가 안내 없음)
        state = SpeakerState::IDLE;
        setLed(0, 0, 0);
        break;
      }
      if (isButtonJustPressed()) {
        Serial.println("버튼 눌림 감지 - 재시도 녹음 시작");
        currentRecordWindowMs += RETRY_EXTRA_MS;
        bool recognized = recordUploadAndPlay(currentRecordWindowMs);
        if (recognized) {
          state = SpeakerState::IDLE;
          setLed(0, 0, 0);
        } else {
          retryDeadlineAt = millis() + RETRY_WINDOW_MS; // 재시도 창 갱신
        }
      }
      break;
    }
  }

  delay(10);
}
