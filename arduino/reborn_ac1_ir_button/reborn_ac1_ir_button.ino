// ReBorn — 카세트형 LG 시스템 에어컨(#1, LT-W1102M3E) 물리 버튼 IR 컨트롤러
// 작성일: 2026-07-29. ⚠️ 실기기 미검증 상태로 배포 — 설치일에 현장 테스트가 불가능해서
// 최대한 실패 지점을 줄이는 방향으로 설계함(WiFi/서버 연동 완전히 배제, 순수 로컬
// 버튼 입력 → IR 송신만 하는 독립형 장치). 설치기사님께 부탁할 필요도 없이, 이 보드를
// 그 실내기 코너 패널(검정 수신 창으로 추정되는 부분)에서 가까운 곳에 두고 전원만
// 연결해두면, 그 자리 누구든 버튼만 누르면 동작한다.
//
// ⚠️⚠️ 확인 안 된 전제 2가지(둘 다 틀리면 이 장치는 반응 없음 — 그래도 안전, 위험한 동작 없음):
//   1. 이 카세트형 실내기가 애초에 IR 수신부를 갖고 있는지 자체가 미확인(사진으로 추정만 함).
//      기본이 유선리모컨(PREMTB 계열)이라 IR 수신부가 아예 없을 수도 있음.
//   2. Arduino-IRremote의 범용 LG 프로토콜(Aircondition_LG)이 이 상업용 시스템에어컨(TW/NW
//      계열)에도 통하는지 미확인 — 이 라이브러리는 LG 가정용 벽걸이/타워형 리모컨을 기준으로
//      리버스 엔지니어링된 것으로 보이고, 상업용 카세트형은 완전히 다른(더 긴 풀스테이트)
//      프로토콜을 쓸 가능성이 있음(실제로 유선 프로토콜은 별도 6~13바이트 방식으로 확인됨).
//   → 안 먹히는 게 오히려 정상적인 결과일 수 있음. 그 경우 옵시디언 설계 문서 6-2절(LIN 버스,
//      벽 유선리모컨 뒷면 단자 태핑) 또는 6-3절(LG 정품 PWFMDD200 WiFi 모듈)로 넘어갈 것.
//
// 라이브러리 설치 필요(Arduino IDE 라이브러리 매니저): "IRremote" (Armin Joachimsmeyer, v4 이상)
// 배선:
//   IR 송신 모듈 SIG → D3
//   버튼 한쪽 다리 → D2, 반대쪽 다리 → GND (INPUT_PULLUP 사용, 버튼에 별도 저항 불필요)
// 사용법: 버튼을 짧게 누르면 냉방 24도로 켜짐, 3초 이상 누르고 있으면 꺼짐.

#include <IRremote.hpp>
#include "ac_LG.hpp"

const uint8_t IR_SEND_PIN = 3;
const uint8_t BUTTON_PIN = 2;
const unsigned long LONG_PRESS_MS = 3000UL;

Aircondition_LG lgAc;

bool isLongPress() {
  unsigned long heldSince = millis();
  while (digitalRead(BUTTON_PIN) == LOW) {
    if (millis() - heldSince > LONG_PRESS_MS) return true;
    delay(20);
  }
  return false;
}

void sendPowerOnCool24() {
  lgAc.sendCommandAndParameter('1', 0);   // 전원 켜기
  delay(300);
  lgAc.sendCommandAndParameter('m', 'c'); // 냉방 모드
  delay(300);
  lgAc.sendCommandAndParameter('t', 24);  // 24도
  Serial.println("전송: 전원 켜기 + 냉방 24도");
}

void sendPowerOff() {
  lgAc.sendCommandAndParameter('0', 0);   // 전원 끄기
  Serial.println("전송: 전원 끄기");
}

void setup() {
  Serial.begin(115200);
  delay(300);
  pinMode(BUTTON_PIN, INPUT_PULLUP);

  IrSender.begin(IR_SEND_PIN);

  // true=벽걸이형 프로토콜. 상업용 카세트형이 벽걸이/타워형 중 어느 쪽 팬속도 매핑에 더
  // 가까운지 불확실 — 이 값으로 먼저 시도하고, 반응이 없으면 false로 바꿔서 재업로드해볼 것.
  lgAc.setType(true);

  Serial.println("준비 완료 — 버튼 짧게 누르면 냉방 24도 켜짐 / 3초 이상 누르면 꺼짐");
}

void loop() {
  if (digitalRead(BUTTON_PIN) == LOW) {
    if (isLongPress()) {
      sendPowerOff();
    } else {
      sendPowerOnCool24();
    }
    delay(500); // 디바운스 — 버튼 뗀 후 다음 입력까지 최소 간격
  }
}
