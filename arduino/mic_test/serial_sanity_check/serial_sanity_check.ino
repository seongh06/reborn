// ATOM Echo가 업로드/실행/시리얼 출력 파이프라인 자체는 정상인지 확인하는 최소 진단 스케치.
// I2S, NeoPixel, 버튼 아무것도 안 쓴다 — 이것조차 안 보이면 mic_test.ino 코드 문제가 아니라
// 업로드/보드 인식/시리얼 보레이트 쪽 문제다.

void setup() {
  Serial.begin(115200); // 921600에서 글자가 깨져서 나와 안전한 값으로 우선 확인(#158)
  delay(300);
  Serial.println("=== 시리얼 정상 동작 확인 스케치 시작 ===");
}

void loop() {
  Serial.print("살아있음, millis=");
  Serial.println(millis());
  delay(500);
}
