#pragma once

// Arduino IDE가 .ino 파일 안 함수 프로토타입을 소스 맨 앞으로 자동 삽입하는데, 그 위치가
// 이 구조체 정의보다 앞이라 "does not name a type" 컴파일 에러가 난다. 별도 헤더로 분리해
// 다른 #include들과 함께 최상단에서 먼저 로드되도록 우회한다.
struct VoiceResponseMeta {
  bool ok = false;
  bool recognized = false;
  long contentLength = -1;
  uint32_t sampleRate = 24000; // 사용 전 항상 parseSampleRate()로 덮어써짐(placeholder일 뿐)
};
