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
//   - "FlashStorage_SAMD" by Khoi Hoang — EEPROM 에뮬레이션 API(EEPROM.put/get/commit)로 SAMD21
//     플래시에 WiFi SSID/비밀번호/기기 시리얼을 저장한다(#143, SoftAP 프로비저닝).
//
// HTTPS 인증서 등록 필요 (최초 1회, 업로드 전에):
//   도구 > "Upload SSL Root Certificates" 실행 → 목록에 "www.reborn-energy.com" 추가 후 업로드.
//   WiFiNINA는 ESP32의 setInsecure() 같은 검증 생략 옵션이 없어서, NINA 모듈에 루트 인증서를
//   미리 올려둬야 HTTPS 연결이 됩니다. 이 과정 없이 업로드하면 서버 연결에서 계속 실패합니다.
//
// WiFi SSID/비밀번호와 기기 ID는 더 이상 하드코딩하지 않는다(#143). 최초 부팅 시 저장된 값이 없으면
// reborn_ai_speaker.ino와 동일한 패턴으로 SoftAP 프로비저닝 포털을 띄워 휴대폰으로 설정을 받는다.
// 서버 등록 선행 필요(#147): 여기 입력하는 값은 실물에 부착된 8자리 시리얼 번호이며, 관리자 앱에서
// 그 시리얼로 기기를 등록해야 POST /api/metric/collect 가 성공한다.
//
// ⚠️ WiFiNINA(SAMD21)에는 ESP32의 <WebServer.h>/<DNSServer.h> 같은 완성된 캡티브 포털 라이브러리가
// 없다. 아래 HTTP 서버(WiFiServer 기반 최소 파서)와 DNS 캡티브 리다이렉트는 손으로 구현했고, 실기기로
// 아직 검증하지 못했다 — 최초 업로드 후 반드시 확인할 것(파일 하단 "실기기 검증 체크리스트" 참고).
// 자동 캡티브 포털 팝업(DNS 스푸핑)은 만들지 않았다 — AP 접속 후 브라우저로 직접 192.168.4.1을 입력해야
// 한다(ESP32판과의 유일한 UX 차이, 의도적 범위 축소).
//
// ⚠️ 이 보드에는 ATOM Echo와 달리 범용 버튼이 배선되어 있지 않다 — 그래서 "버튼 3초 홀드로 재설정"
// 기능이 없다. 재프로비저닝(WiFi를 바꾸고 싶을 때)은 현재 재업로드로만 가능하다. 저장된 WiFi 연결에
// 실패했을 때는 부팅 시 자동으로 포털로 폴백한다.
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
#include <FlashStorage_SAMD.h>

// ===== 설정값 =====
const char *SERVER_HOST = "www.reborn-energy.com";
const int SERVER_PORT = 443;

String g_wifiSsid;
String g_wifiPassword;
String g_deviceId; // 서버에 등록된 deviceKey(시리얼 번호)와 반드시 일치해야 함

#define DHTPIN 2 // DHT22 Signal 핀 — Nano 33 IoT의 D2 (특별 기능 없는 안전한 범용 핀)
#define DHTTYPE DHT22

const unsigned long SEND_INTERVAL_MS = 60UL * 1000UL; // 60초마다 전송

// ===== 전역 상태 =====
DHT dht(DHTPIN, DHTTYPE);
WiFiSSLClient wifiClient;
HttpClient httpClient(wifiClient, SERVER_HOST, SERVER_PORT);
unsigned long lastSendAt = 0;

// ===== SoftAP 프로비저닝(#143) =====
// FlashStorage_SAMD가 제공하는 EEPROM 에뮬레이션(EEPROM.put/get/commit)으로 저장한다.
// magic 값으로 유효성을 검사하는 이유: 최초 플래싱 시 플래시 내용은 결정되지 않은(비어있지 않을 수
// 있는) 값이라, 단순 bool 플래그라면 우연히 true로 읽혀 잘못된 빈 SSID로 연결을 시도할 위험이 있다.
struct ProvisioningData {
  uint32_t magic;
  char ssid[33];
  char pass[64];
  char deviceId[16];
};

const uint32_t PROVISIONING_MAGIC = 0x5242314E; // "RB1N"
const int PROVISIONING_EEPROM_ADDR = 0;

bool loadProvisioning() {
  ProvisioningData data;
  EEPROM.get(PROVISIONING_EEPROM_ADDR, data);
  if (data.magic != PROVISIONING_MAGIC) return false;

  data.ssid[sizeof(data.ssid) - 1] = '\0';
  data.pass[sizeof(data.pass) - 1] = '\0';
  data.deviceId[sizeof(data.deviceId) - 1] = '\0';
  g_wifiSsid = String(data.ssid);
  g_wifiPassword = String(data.pass);
  g_deviceId = String(data.deviceId);
  return g_wifiSsid.length() > 0 && g_deviceId.length() > 0;
}

void saveProvisioning(const String &ssid, const String &password, const String &deviceId) {
  ProvisioningData data;
  data.magic = PROVISIONING_MAGIC;
  ssid.toCharArray(data.ssid, sizeof(data.ssid));
  password.toCharArray(data.pass, sizeof(data.pass));
  deviceId.toCharArray(data.deviceId, sizeof(data.deviceId));

  EEPROM.put(PROVISIONING_EEPROM_ADDR, data);
  EEPROM.commit();
}

// 기기별 고유 AP 비밀번호 — reborn_ai_speaker.ino와 동일한 이유(공용 고정 비밀번호는 근처의 누구나
// 재기록할 수 있음)로 MAC 기반 고유값을 쓴다. WiFiNINA는 ESP32의 getEfuseMac() 대응이 없어
// WiFi.macAddress(byte*)로 대체 — 반환되는 바이트 순서(MSB/LSB)는 미검증이라 순서가 달라도 무방하게
// 그냥 6바이트를 그대로 이어붙인다(고유성만 필요하고 실제 MAC 표기 순서는 중요하지 않음).
String buildDeviceApPassword() {
  byte mac[6];
  WiFi.macAddress(mac);
  char buf[16];
  snprintf(buf, sizeof(buf), "rb-%02x%02x%02x%02x%02x%02x", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
  return String(buf); // "rb-" + 12자 = 15자(WPA2 8자 이상 충족)
}

String buildApSsidSuffix() {
  byte mac[6];
  WiFi.macAddress(mac);
  char buf[5];
  snprintf(buf, sizeof(buf), "%02X%02X", mac[4], mac[5]);
  return String(buf);
}

// %xx/+ 인코딩을 되돌린다 — 프로비저닝 폼이 GET 쿼리스트링으로 값을 받기 때문에 WiFi
// 비밀번호에 공백/특수문자가 있으면 반드시 디코딩해야 한다.
String urlDecode(const String &input) {
  String decoded = "";
  for (unsigned int i = 0; i < input.length(); i++) {
    char c = input[i];
    if (c == '+') {
      decoded += ' ';
    } else if (c == '%' && i + 2 < input.length()) {
      char hex[3] = { input[i + 1], input[i + 2], '\0' };
      decoded += (char)strtol(hex, NULL, 16);
      i += 2;
    } else {
      decoded += c;
    }
  }
  return decoded;
}

String getQueryParam(const String &query, const String &key) {
  String needle = key + "=";
  int start = query.indexOf(needle);
  if (start < 0) return "";
  start += needle.length();
  int end = query.indexOf('&', start);
  if (end < 0) end = query.length();
  return urlDecode(query.substring(start, end));
}

// 요청 라인(예: "GET /save?ssid=... HTTP/1.1") 한 줄만 읽는다 — 캡티브 포털이 아니라 최소 파서라
// 헤더 값(Content-Type 등)은 쓰지 않는다.
String readLine(WiFiClient &client, unsigned long timeoutMs) {
  unsigned long deadline = millis() + timeoutMs;
  String line = "";
  while (client.connected() && millis() < deadline) {
    if (client.available()) {
      char c = client.read();
      if (c == '\n') break;
      if (c != '\r') line += c;
    }
  }
  return line;
}

// 요청 라인 이후 남은 헤더를 다 읽어서 버린다 — 응답을 쓰기 전에 소켓에 남은 바이트를 비워야
// 일부 브라우저/클라이언트가 응답을 제대로 받는다(빈 줄이 헤더의 끝).
void drainHeaders(WiFiClient &client) {
  for (int i = 0; i < 50; i++) {
    String line = readLine(client, 1000UL);
    if (line.length() == 0) break;
  }
}

void sendHttpResponse(WiFiClient &client, const String &body) {
  client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: text/html; charset=utf-8");
  client.print("Content-Length: ");
  client.println(body.length());
  client.println("Connection: close");
  client.println();
  client.print(body);
}

String buildProvisioningFormHtml() {
  return String(
    "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
    "<title>ReBorn 기기 설정</title></head>"
    "<body style=\"font-family:sans-serif;padding:20px;\">"
    "<h2>ReBorn 아두이노 설정</h2>"
    "<p>이 페이지가 자동으로 뜨지 않으면 브라우저에서 직접 <b>192.168.4.1</b> 로 접속하세요.</p>"
    "<form action=\"/save\" method=\"GET\">"
    "WiFi SSID (2.4GHz만 지원)<br><input type=\"text\" name=\"ssid\" required><br><br>"
    "WiFi 비밀번호<br><input type=\"password\" name=\"password\"><br><br>"
    "시리얼 번호 (기기에 부착된 8자리 코드)<br><input type=\"text\" name=\"device_id\" required><br><br>"
    "<button type=\"submit\">저장하고 재부팅</button>"
    "</form></body></html>"
  );
}

// 블로킹 — 설정이 저장되어 재부팅될 때까지 리턴하지 않는다.
void runProvisioningPortal() {
  String apSsid = "ReBorn-Setup-" + buildApSsidSuffix();
  String apPassword = buildDeviceApPassword();

  int apStatus = WiFi.beginAP(apSsid.c_str(), apPassword.c_str());
  if (apStatus != WL_AP_LISTENING) {
    Serial.println("AP 모드 시작 실패 — 계속 재시도됨");
  }
  delay(1000); // AP 안정화 대기

  WiFiServer server(80);
  server.begin();

  Serial.printf("프로비저닝 포털 시작 — SSID: %s (PW: %s), http://192.168.4.1/\n",
                apSsid.c_str(), apPassword.c_str());

  while (true) {
    WiFiClient client = server.available();
    if (!client) {
      delay(10);
      continue;
    }

    String requestLine = readLine(client, 3000UL);
    drainHeaders(client);

    int firstSpace = requestLine.indexOf(' ');
    int secondSpace = requestLine.indexOf(' ', firstSpace + 1);
    String pathAndQuery = (firstSpace >= 0 && secondSpace > firstSpace)
      ? requestLine.substring(firstSpace + 1, secondSpace)
      : "/";

    int qmark = pathAndQuery.indexOf('?');
    String path = qmark >= 0 ? pathAndQuery.substring(0, qmark) : pathAndQuery;
    String query = qmark >= 0 ? pathAndQuery.substring(qmark + 1) : "";

    if (path == "/save") {
      String ssid = getQueryParam(query, "ssid");
      String password = getQueryParam(query, "password");
      String deviceId = getQueryParam(query, "device_id");

      if (ssid.length() > 0 && deviceId.length() > 0) {
        saveProvisioning(ssid, password, deviceId);
        sendHttpResponse(client, "<html><body>저장 완료 — 기기가 재부팅됩니다.</body></html>");
        client.stop();
        delay(1000);
        NVIC_SystemReset();
      }
      sendHttpResponse(client, buildProvisioningFormHtml());
    } else {
      sendHttpResponse(client, buildProvisioningFormHtml());
    }

    client.stop();
  }
}

// CodeRabbit 리뷰(reborn_ai_speaker.ino PR #144) 지적을 동일하게 반영: 무한 대기 대신 타임아웃을 두고
// 실패 시 false를 반환해 호출부가 재시도/프로비저닝 포털 폴백을 결정하게 한다.
bool connectWiFi(unsigned long timeoutMs) {
  if (WiFi.status() == WL_CONNECTED) return true;

  Serial.print("WiFi 연결 중");
  WiFi.begin(g_wifiSsid.c_str(), g_wifiPassword.c_str());
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

void sendMetric(float temperature, float humidity) {
  // 부팅 후 정상 운영 중 일시적으로 WiFi가 끊겼다고 바로 포털을 띄우면 사람이 다시 가서 설정해야
  // 하니, 여기서는 재시도만 하고 실패하면 이번 전송만 건너뛴다(포털 폴백은 setup()의 최초 연결
  // 실패 시에만 트리거).
  if (!connectWiFi(10000UL)) {
    Serial.println("WiFi 미연결 상태 — 이번 전송을 건너뜁니다");
    return;
  }

  char body[128];
  snprintf(body, sizeof(body), "{\"temperature\":%.2f,\"humidity\":%.2f}", temperature, humidity);

  httpClient.beginRequest();
  httpClient.post("/api/metric/collect");
  httpClient.sendHeader("Content-Type", "application/json");
  httpClient.sendHeader("X-Device-Id", g_deviceId.c_str());
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

  if (!loadProvisioning()) {
    Serial.println("저장된 WiFi/기기 설정 없음 — 프로비저닝 포털 시작");
    runProvisioningPortal(); // 저장 완료 시 내부에서 재부팅되어 반환하지 않음
  }

  if (!connectWiFi(20000UL)) {
    Serial.println("WiFi 연결 실패 — 저장된 자격 증명이 잘못됐을 수 있어 프로비저닝 포털로 폴백");
    runProvisioningPortal(); // 저장 완료 시 내부에서 재부팅되어 반환하지 않음
  }

  readAndSend(); // 부팅 직후 1회 즉시 전송(대기 없이 바로 확인 가능하도록)
  lastSendAt = millis();
}

void loop() {
  if (millis() - lastSendAt >= SEND_INTERVAL_MS) {
    lastSendAt = millis();
    readAndSend();
  }
}

// ===== 실기기 검증 체크리스트 (다음 세션, 보드 확보 후) =====
// - [ ] 최초 부팅 시 AP(ReBorn-Setup-XXXX)가 뜨고 192.168.4.1로 폼 접속되는지
// - [ ] 폼 제출 후 저장되고 재부팅되는지, 재부팅 후에도 값이 유지되는지(FlashStorage_SAMD 영속성)
// - [ ] 저장된 WiFi 비밀번호가 틀렸을 때 20초 타임아웃 후 포털로 폴백하는지
// - [ ] urlDecode()가 공백/특수문자 포함 비밀번호를 깨트리지 않는지
// - [ ] WiFi.macAddress() 반환 바이트 순서와 무관하게 AP 비밀번호/SSID가 매 부팅 동일하게 나오는지
