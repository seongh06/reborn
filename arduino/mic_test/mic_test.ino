// ReBorn — AI 스피커(ATOM Echo) 마이크 녹음 품질 확인용 임시 테스트 스케치.
//
// 목적: reborn_ai_speaker.ino의 실제 서버 왕복(HTTPS 업로드 + Gemini 분석) 없이, 마이크로 들어온
// 소리가 실제로 어느 정도 잘 잡히는지(음량/노이즈/명료도) 바로 확인한다.
//
// 동작: 버튼을 누르면 "띠링" → 10초간 마이크로 녹음하면서 동시에 (1) 그 자리에서 바로 스피커로
// 흘려보내 즉석에서 들려주고, (2) 같은 오디오를 WAV 형식으로 USB Serial에도 그대로 흘려보낸다 →
// 종료 시 "띠링" 다시 재생. (2)를 컴퓨터에서 `save_recording.py`로 받으면 실제 WAV 파일로 저장되어
// 나중에 다시 들어보거나 다른 사람에게 공유할 수 있다.
//
// ATOM Echo는 PSRAM이 없어서(reborn_ai_speaker.ino와 동일 제약) 10초치 오디오를 통버퍼링하지 않고
// 작은 청크 단위로 즉시 스피커+Serial 양쪽에 흘려보낸다 — 이번엔 녹음 길이가 고정(10초)이라 실제
// WAV 헤더에 정확한 데이터 크기를 채워 넣을 수 있다(스트리밍 placeholder 필요 없음).
//
// 사용법:
//   1. 이 폴더(mic_test)만 Arduino IDE로 업로드 (reborn_ai_speaker.ino와는 별개의 독립 스케치)
//   2. 컴퓨터에서 `python save_recording.py <포트> recording.wav` 실행 (pyserial 필요: pip install pyserial)
//   3. 기기 버튼을 눌러 10초간 말하기 → 스크립트가 자동으로 recording.wav 저장
//
// ⚠️ WiFi/서버 연동 전혀 없는 독립 스케치.
// ⚠️ 미검증: I2S RX/TX를 같은 루프에서 동시에(진짜 풀듀플렉스로) 쓰는 게 이 보드/라이브러리 조합에서
//    문제없이 되는지, 그리고 921600bps Serial이 32KB/s 오디오 전송을 안정적으로 따라가는지 모두
//    실기기로 확인 전이다 — 소리가 끊기거나 파일이 깨지면 이 부분부터 의심할 것.

#include <Adafruit_NeoPixel.h>
#include <ESP_I2S.h>
#include <math.h>

// ===== 핀 정의 (ATOM Echo 고정, reborn_ai_speaker.ino와 동일) =====
#define I2S_BCK_PIN       19
#define I2S_LRCK_PIN      33
#define I2S_DATA_OUT_PIN  22 // 스피커
#define I2S_DATA_IN_PIN   23 // 마이크
#define BUTTON_PIN        39
#define LED_PIN           27

#define SAMPLE_RATE 16000
#define STREAM_CHUNK_BYTES 1024

// 버튼/칩 동작 확인 완료 후 다시 921600으로 복구 — save_recording.py의 BAUD_RATE(921600)와
// 반드시 같아야 한다. 16kHz*16bit=32KB/s 오디오 전송에 115200bps는 부족하다.
// Arduino IDE 시리얼 모니터로 디버그 텍스트를 볼 땐 모니터 보레이트도 921600으로(커스텀 입력 필요할 수 있음).
#define SERIAL_BAUD_RATE 921600

const unsigned long TEST_RECORD_MS = 10000UL; // 테스트 녹음 길이 — 필요하면 조절(초 단위 아님, ms)
const unsigned long BUTTON_DEBOUNCE_MS = 50UL;

Adafruit_NeoPixel pixel(1, LED_PIN, NEO_GRB + NEO_KHZ800);
I2SClass i2s;

static uint8_t streamBuf[STREAM_CHUNK_BYTES];

bool lastButtonRaw = HIGH; // active-LOW, 평상시 HIGH
bool lastButtonStable = HIGH;
unsigned long lastButtonChangeAt = 0;

void setLed(uint8_t r, uint8_t g, uint8_t b) {
  pixel.setPixelColor(0, pixel.Color(r, g, b));
  pixel.show();
}

bool isButtonJustPressed() {
  bool raw = digitalRead(BUTTON_PIN);
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

// reborn_ai_speaker.ino의 playTone/playChime과 동일한 방식(로컬 사인파 생성, 서버 응답 아님).
// 진폭을 INT16 최대치(32767)로 잡아 최대 음량으로 재생 — 순수 사인파라 이 값 자체로는 클리핑이
// 안 생기지만(sinf가 -1..1 범위), 그만큼 앰프/스피커가 낼 수 있는 최대치라 계속 이 값으로 쓰면
// 다른 목적(예: TTS 등 실제 음성 데이터)에는 과할 수 있음 — 이 테스트 스케치 한정.
void playTone(float freqHz, unsigned long durationMs) {
  const size_t totalSamples = (size_t)(SAMPLE_RATE * durationMs / 1000UL);
  const float phaseInc = 2.0f * PI * freqHz / (float)SAMPLE_RATE;
  float phase = 0.0f;

  int16_t chunk[STREAM_CHUNK_BYTES / 2];
  const size_t chunkCapacity = sizeof(chunk) / sizeof(chunk[0]);

  i2s.configureTX(SAMPLE_RATE, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);

  size_t samplesWritten = 0;
  while (samplesWritten < totalSamples) {
    size_t chunkSamples = min(chunkCapacity, totalSamples - samplesWritten);
    for (size_t i = 0; i < chunkSamples; i++) {
      chunk[i] = (int16_t)(sinf(phase) * 1000.0f);
      phase += phaseInc;
      if (phase > 2.0f * PI) phase -= 2.0f * PI;
    }
    i2s.write((uint8_t *)chunk, chunkSamples * sizeof(int16_t));
    samplesWritten += chunkSamples;
  }
}

void playChime() {
  playTone(880.0f, 100);
  playTone(1318.0f, 150);
}

// 이번엔 녹음 길이가 고정이라(스트리밍 아님) 실제 데이터 크기를 정확히 채운 표준 WAV 헤더를 쓴다 —
// reborn_ai_speaker.ino의 buildWavHeader()는 길이를 몰라 0xFFFFFFFF placeholder를 쓰는 것과 다름.
size_t buildWavHeaderExact(uint8_t *out, uint32_t sampleRate, uint32_t dataBytes) {
  const uint16_t bitsPerSample = 16;
  const uint16_t numChannels = 1;
  uint32_t byteRate = sampleRate * numChannels * (bitsPerSample / 8);
  uint16_t blockAlign = numChannels * (bitsPerSample / 8);
  uint32_t riffSize = 36 + dataBytes;

  memcpy(out + 0, "RIFF", 4);
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
  memcpy(out + 40, &dataBytes, 4);

  return 44;
}

// 마이크로 durationMs만큼 녹음하면서 동시에 (1)스피커로 즉석 재생 (2)Serial로 WAV 스트리밍.
// PSRAM이 없어 통버퍼링 대신 청크 단위로 양쪽에 바로 흘려보낸다.
void recordAndStream(unsigned long durationMs) {
  const uint32_t dataBytes = (uint32_t)(SAMPLE_RATE * 2 * (durationMs / 1000UL));

  uint8_t wavHeader[44];
  buildWavHeaderExact(wavHeader, SAMPLE_RATE, dataBytes);

  Serial.println("===AUDIO_START===");
  Serial.write(wavHeader, sizeof(wavHeader));

  i2s.configureRX(SAMPLE_RATE, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);
  i2s.configureTX(SAMPLE_RATE, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);

  uint32_t bytesSent = 0;
  while (bytesSent < dataBytes) {
    size_t want = (size_t)min((uint32_t)STREAM_CHUNK_BYTES, dataBytes - bytesSent);
    size_t bytesRead = i2s.readBytes((char *)streamBuf, want);
    if (bytesRead == 0) continue;

    i2s.write(streamBuf, bytesRead);      // 즉석 스피커 재생(모니터링용)
    Serial.write(streamBuf, bytesRead);   // 컴퓨터로 WAV 데이터 전송
    bytesSent += bytesRead;
  }

  Serial.println();
  Serial.println("===AUDIO_END===");
}

void setup() {
  Serial.begin(SERIAL_BAUD_RATE);
  delay(300);

  pinMode(BUTTON_PIN, INPUT);
  pixel.begin();
  setLed(0, 0, 0);

  i2s.setPins(I2S_BCK_PIN, I2S_LRCK_PIN, I2S_DATA_OUT_PIN, I2S_DATA_IN_PIN);
  bool ok = i2s.begin(I2S_MODE_STD, SAMPLE_RATE, I2S_DATA_BIT_WIDTH_16BIT, I2S_SLOT_MODE_MONO);
  if (!ok) {
    Serial.println("I2S 초기화 실패");
  }

  Serial.println("마이크 테스트 준비 완료 — 버튼을 눌러 10초 녹음을 시작하세요");
}

// ⚠️ 임시 디버그용 — 버튼이 아예 안 눌리는 것처럼 보일 때, 디바운스 로직을 거치지 않고 GPIO 원시
// 값을 그대로 찍어서 하드웨어/배선 문제인지 소프트웨어 로직 문제인지 구분한다. 문제 해결되면 지워도 됨.
unsigned long lastRawPrintAt = 0;

void loop() {
  if (millis() - lastRawPrintAt > 200UL) {
    lastRawPrintAt = millis();
    Serial.print("[DEBUG] raw digitalRead(BUTTON_PIN) = ");
    Serial.println(digitalRead(BUTTON_PIN));
  }

  if (isButtonJustPressed()) {
    Serial.println("버튼 눌림 - 마이크 테스트 시작");
    setLed(0, 0, 40); // 파랑 = 테스트 중
    playChime();
    recordAndStream(TEST_RECORD_MS);
    playChime();
    setLed(0, 0, 0);
    Serial.println("테스트 종료");
  }
  delay(10);
}
