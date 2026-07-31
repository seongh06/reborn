#pragma once

// AI 스피커(#142) 재생 음량 부스트용 소프트웨어 게인 유틸리티.
//
// ATOM Echo 온보드 앰프(NS4168)는 게인이 고정된 클래스D 앰프라 I2C 등으로 볼륨을 조절할 방법이 없다.
// 그래서 I2S로 내보내기 직전, 16비트 PCM 샘플 값 자체에 배율(GAIN)을 곱해 더 크게 만드는 방식으로
// 우회한다 — 사람 귀에 들리는 음량이 커지는 대신, 배율을 과하게 높이면 INT16 범위를 넘는 샘플이
// 잘려나가(clipping) 찌그러진 소리가 날 수 있다. 실기기로 들어보기 전까지 GAIN 값은 추정치다.
//
// 사용법: reborn_ai_speaker.ino에서 스트리밍 재생 직전 applyGain(streamBuf, n)을 호출.

#include <stdint.h>
#include <stddef.h>

// 실기기로 들어보면서 이 값만 조절하면 됨. 1.0 = 원본 그대로, 커질수록 크지만 찌그러짐 위험도 커짐.
// 사용자 요청으로 최대 음량으로 올림(2026-08-01) - 이 값 이상으로는 대부분의 입력이 이미 INT16
// 범위를 넘어 클리핑되므로(아래 clamp) 사실상 더 키워도 차이가 없는 "포화점" 근처다. 대신 피크가
// 거의 항상 잘려서 찌그러진 소리가 날 수 있음 - 소리는 커지지만 음질은 떨어지는 트레이드오프.
constexpr float SPEAKER_GAIN = 10.0f;

// buf: 16비트 PCM(리틀 엔디안) 바이트 버퍼. len이 홀수면 마지막 1바이트는 손대지 않는다(청크 경계에서
// 샘플이 반으로 잘려 들어오는 드문 경우 대비 — 기존 스트리밍 코드도 이 경계는 원래 신경 쓰지 않음).
// 제자리(in-place)로 게인을 곱하고 INT16 범위로 클리핑한다.
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
