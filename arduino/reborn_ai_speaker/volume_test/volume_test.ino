// ReBorn — AI 스피커 스피커 볼륨(SPEAKER_GAIN) 테스트 전용 임시 스케치(#276)
// 보드: M5Stack ATOM Echo (reborn_ai_speaker.ino와 동일)
//
// 목적: 녹음→Gemini 분석 전체 흐름 없이, 고정 문구("안녕하세요.") TTS만 버튼 누를 때마다
// 반복 재생해서 SPEAKER_GAIN 값을 빠르게 비교 테스트하기 위함. 서버의 임시 디버그 엔드포인트
// GET /api/feedback/voice/test-tts를 사용한다(reborn_ai_speaker.ino와는 별도 스케치라 실물에
// 둘 중 하나만 매번 업로드해서 전환해야 함 - 같은 기기에 동시에 올라가진 않음).
//
// 전제조건: 이 기기가 reborn_ai_speaker.ino로 이미 WiFi 프로비저닝을 마친 상태여야 한다(NVS에
// 저장된 SSID/비밀번호를 그대로 읽기만 하고, 이 스케치엔 프로비저닝 포털이 없음).
//
// 사용법: 이 스케치 업로드 → 자동으로 WiFi 연결 → 버튼 누를 때마다 "안녕하세요." 재생.
// SPEAKER_GAIN 값을 바꿔가며 재업로드해서 비교. 테스트 끝나면 reborn_ai_speaker.ino로 다시
// 업로드해서 원래 볼륨 값을 그쪽 audio_gain.h에도 반영할 것.

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <Preferences.h>
#include <Adafruit_NeoPixel.h>
#include <ESP_I2S.h>

// ===== 여기 값만 바꿔가며 테스트 =====
// reborn_ai_speaker.ino의 audio_gain.h와 항상 같은 값으로 맞춰서 실제 배포 볼륨을 정확히
// 미리 들어볼 것 - 여기서 맘에 드는 값을 찾으면 그 값을 audio_gain.h의 SPEAKER_GAIN에도 반영.
constexpr float SPEAKER_GAIN = 10.0f;

const char *SERVER_HOST = "www.reborn-energy.com";
const uint16_t SERVER_PORT = 443;

#define I2S_BCK_PIN       19
#define I2S_LRCK_PIN      33
#define I2S_DATA_OUT_PIN  22
#define I2S_DATA_IN_PIN   23
#define BUTTON_PIN        39
#define LED_PIN           27

#define DEFAULT_PLAYBACK_SAMPLE_RATE 24000
#define STREAM_CHUNK_BYTES 1024

Adafruit_NeoPixel pixel(1, LED_PIN, NEO_GRB + NEO_KHZ800);
I2SClass i2s;
static uint8_t streamBuf[STREAM_CHUNK_BYTES];

String g_wifiSsid;
String g_wifiPassword;

bool lastButtonRaw = HIGH;
bool lastButtonStable = HIGH;
unsigned long lastButtonChangeAt = 0;

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

void setLed(uint8_t r, uint8_t g, uint8_t b) {
  pixel.setPixelColor(0, pixel.Color(r, g, b));
  pixel.show();
}

bool loadProvisioning() {
  Preferences prefs;
  prefs.begin("reborn", true);
  g_wifiSsid = prefs.getString("ssid", "");
  g_wifiPassword = prefs.getString("pass", "");
  prefs.end();
  return g_wifiSsid.length() > 0;
}

bool connectWiFi(unsigned long timeoutMs) {
  if (WiFi.status() == WL_CONNECTED) return true;
  Serial.print("WiFi 연결 중");
  WiFi.begin(g_wifiSsid.c_str(), g_wifiPassword.c_str());
  setLed(20, 0, 20);
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

uint32_t parseSampleRate(const String &contentType) {
  int idx = contentType.indexOf("rate=");
  if (idx < 0) return DEFAULT_PLAYBACK_SAMPLE_RATE;
  int start = idx + 5;
  int end = start;
  while (end < (int)contentType.length() && isDigit(contentType[end])) end++;
  if (end == start) return DEFAULT_PLAYBACK_SAMPLE_RATE;
  return (uint32_t)contentType.substring(start, end).toInt();
}

inline void applyGain(uint8_t *buf, size_t len, float gain = SPEAKER_GAIN) {
  size_t sampleCount = len / 2;
  int16_t *samples = reinterpret_cast<int16_t *>(buf);
  for (size_t i = 0; i < sampleCount; i++) {
    int32_t boosted = (int32_t)(samples[i] * gain);
    if (boosted > INT16_MAX) boosted = INT16_MAX;
    if (boosted < INT16_MIN) boosted = INT16_MIN;
    samples[i] = (int16_t)boosted;
  }
}

// GET /api/feedback/voice/test-tts 호출 후 응답 오디오를 즉시 스트리밍 재생 (안녕하세요.)
void playTestTts() {
  WiFiClientSecure client;
  client.setCACert(SERVER_ROOT_CA_PEM);

  setLed(40, 40, 0);
  if (!client.connect(SERVER_HOST, SERVER_PORT)) {
    Serial.println("서버 연결 실패");
    setLed(40, 0, 0);
    delay(500);
    setLed(0, 0, 0);
    return;
  }

  client.print("GET /api/feedback/voice/test-tts HTTP/1.1\r\n");
  client.printf("Host: %s\r\n", SERVER_HOST);
  client.print("Connection: close\r\n\r\n");

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
    Serial.print("응답 실패(상태줄): ");
    Serial.println(statusLine);
    client.stop();
    setLed(40, 0, 0);
    delay(500);
    setLed(0, 0, 0);
    return;
  }

  long contentLength = -1;
  String contentType = "";
  bool headersEnded = false;
  unsigned long lastDataAt = millis();
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
      break;
    }
    String lower = line;
    lower.toLowerCase();
    if (lower.startsWith("content-length:")) {
      contentLength = line.substring(line.indexOf(':') + 1).toInt();
    } else if (lower.startsWith("content-type:")) {
      contentType = line.substring(line.indexOf(':') + 1);
      contentType.trim();
    }
  }
  if (!headersEnded || contentLength <= 0) {
    Serial.println("응답 헤더 파싱 실패");
    client.stop();
    setLed(40, 0, 0);
    delay(500);
    setLed(0, 0, 0);
    return;
  }

  uint32_t sampleRate = parseSampleRate(contentType);
  i2s.configureTX(sampleRate, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);
  setLed(0, 40, 0);

  long remaining = contentLength;
  lastDataAt = millis();
  while (remaining > 0 && client.connected() && millis() - lastDataAt < 5000UL) {
    if (!client.available()) {
      delay(2);
      continue;
    }
    int toRead = (int)min((long)STREAM_CHUNK_BYTES, remaining);
    int n = client.read(streamBuf, toRead);
    if (n <= 0) continue;
    lastDataAt = millis();
    applyGain(streamBuf, (size_t)n);
    i2s.write(streamBuf, (size_t)n);
    remaining -= n;
  }

  client.stop();
  setLed(0, 0, 0);
  Serial.printf("재생 완료 (gain=%.1f)\n", SPEAKER_GAIN);
}

bool isButtonJustPressed() {
  bool raw = digitalRead(BUTTON_PIN);
  unsigned long now = millis();
  if (raw != lastButtonRaw) {
    lastButtonRaw = raw;
    lastButtonChangeAt = now;
  }
  bool justPressed = false;
  if (now - lastButtonChangeAt > 50UL && lastButtonStable != raw) {
    lastButtonStable = raw;
    if (lastButtonStable == LOW) justPressed = true;
  }
  return justPressed;
}

void setup() {
  Serial.begin(115200);
  delay(300);
  pinMode(BUTTON_PIN, INPUT);
  pixel.begin();
  setLed(0, 0, 0);

  if (!loadProvisioning()) {
    Serial.println("저장된 WiFi 정보 없음 - 먼저 reborn_ai_speaker.ino로 프로비저닝부터 완료할 것");
    setLed(40, 0, 0);
    return;
  }

  i2s.setPins(I2S_BCK_PIN, I2S_LRCK_PIN, I2S_DATA_OUT_PIN, I2S_DATA_IN_PIN);
  i2s.begin(I2S_MODE_STD, DEFAULT_PLAYBACK_SAMPLE_RATE, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);

  if (!connectWiFi(20000UL)) {
    Serial.println("WiFi 연결 실패 - 저장된 정보가 최신인지 확인할 것");
    setLed(40, 0, 0);
    return;
  }

  Serial.printf("준비 완료 (gain=%.1f) - 버튼을 눌러 \"안녕하세요.\" 재생\n", SPEAKER_GAIN);
}

void loop() {
  connectWiFi(5000UL);
  if (isButtonJustPressed()) {
    Serial.println("버튼 눌림 - 재생 시작");
    playTestTts();
  }
}
