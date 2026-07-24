#!/usr/bin/env python3
"""
ReBorn — mic_test.ino가 USB Serial로 흘려보내는 WAV 오디오를 받아 컴퓨터에 파일로 저장한다.

사용법:
    pip install pyserial
    python save_recording.py <포트> [저장할파일.wav]

    포트 예시: Windows는 COM5, macOS/Linux는 /dev/tty.usbserial-XXXX 등
    저장 경로를 생략하면 recording.wav로 저장한다.

동작: 기기가 "===AUDIO_START===" 마커를 보내면 그 뒤에 오는 정확히 (44바이트 WAV 헤더 +
16kHz*16bit*모노*10초 분량) 바이트를 그대로 받아서 파일에 쓴다 — mic_test.ino가 보내는 헤더에
이미 정확한 데이터 크기가 들어있으므로 그 바이트 수만큼만 읽으면 된다.
"""
import sys
import time

try:
    import serial
except ImportError:
    print("pyserial이 필요합니다: pip install pyserial")
    sys.exit(1)

BAUD_RATE = 921600  # mic_test.ino의 SERIAL_BAUD_RATE와 반드시 동일해야 함
SAMPLE_RATE = 16000
DURATION_SEC = 10  # mic_test.ino의 TEST_RECORD_MS(ms)와 동일한 초 단위 값으로 맞출 것
WAV_HEADER_SIZE = 44
DATA_BYTES = SAMPLE_RATE * 2 * DURATION_SEC  # 16bit(2byte) mono


def main():
    if len(sys.argv) < 2:
        print("사용법: python save_recording.py <포트> [저장할파일.wav]")
        sys.exit(1)

    port = sys.argv[1]
    out_path = sys.argv[2] if len(sys.argv) > 2 else "recording.wav"

    print(f"{port} 연결 중 (baud={BAUD_RATE})...")

    # ESP32 auto-reset 회로 때문에 open() 시점의 DTR/RTS 상태에 따라 보드가 리셋되거나
    # 부트로더/리셋 상태에 계속 붙잡혀 있을 수 있다. False/False로는 안 풀렸어서 반대 극성(True/True)을
    # 시도한다 — 보드/드라이버(CP210x 등)마다 극성이 달라서 실기기로 맞춰봐야 한다.
    ser = serial.Serial()
    ser.port = port
    ser.baudrate = BAUD_RATE
    ser.timeout = 5
    ser.dtr = True
    ser.rts = True
    ser.open()
    time.sleep(2)
    ser.reset_input_buffer()

    # 보드가 실제로 살아서 뭔가 찍고 있는지(setup() 메시지 등) 버튼을 기다리기 전에 먼저 확인한다.
    print("초기 상태 확인 중 (2초)...")
    ser.timeout = 0.5
    start = time.time()
    saw_anything = False
    while time.time() - start < 2.0:
        line = ser.readline().decode(errors="ignore").strip()
        if line:
            saw_anything = True
            print(f"[기기] {line}")
    if not saw_anything:
        print("⚠️ 아무 것도 안 찍힘 — 보드가 멈춰있거나 아직 리셋 중일 수 있습니다.")
    ser.timeout = 5

    print("기기 버튼을 눌러 녹음을 시작하세요. 시작 신호 대기 중...")
    while True:
        line = ser.readline().decode(errors="ignore").strip()
        if not line:
            continue
        print(f"[기기] {line}")
        if "AUDIO_START" in line:
            break

    total_bytes = WAV_HEADER_SIZE + DATA_BYTES
    print(f"오디오 데이터 수신 중 ({total_bytes}바이트, 약 {DURATION_SEC}초)...")
    audio_data = ser.read(total_bytes)

    if len(audio_data) < total_bytes:
        print(
            f"경고: {total_bytes}바이트를 기대했는데 {len(audio_data)}바이트만 받았습니다 "
            "(연결 끊김/타임아웃 — baud rate가 mic_test.ino와 같은지, 케이블 연결을 확인하세요)."
        )

    with open(out_path, "wb") as f:
        f.write(audio_data)

    print(f"저장 완료: {out_path}")
    ser.close()


if __name__ == "__main__":
    main()
