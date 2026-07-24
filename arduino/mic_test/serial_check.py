#!/usr/bin/env python3
"""
범용 시리얼 출력 확인용 스크립트 — 특정 스케치(mic_test 등)에 종속되지 않고, 그냥 지정한 포트/보레이트로
들어오는 텍스트를 계속 그대로 출력한다. ESP32 auto-reset 회로 문제를 피하려고 open() 전에 DTR/RTS를
미리 세팅한다.

사용법:
    python serial_check.py <포트> [보레이트(기본 115200)]

Ctrl+C로 종료.
"""
import sys
import time

try:
    import serial
except ImportError:
    print("pyserial이 필요합니다: pip install pyserial")
    sys.exit(1)


def main():
    if len(sys.argv) < 2:
        print("사용법: python serial_check.py <포트> [보레이트]")
        sys.exit(1)

    port = sys.argv[1]
    baud = int(sys.argv[2]) if len(sys.argv) > 2 else 115200

    print(f"{port} 연결 중 (baud={baud})...")
    ser = serial.Serial()
    ser.port = port
    ser.baudrate = baud
    ser.timeout = 1
    ser.dtr = False
    ser.rts = False
    ser.open()
    time.sleep(2)
    ser.reset_input_buffer()

    print("연결됨 — 들어오는 내용을 그대로 출력합니다 (Ctrl+C로 종료)")
    try:
        while True:
            line = ser.readline().decode(errors="ignore").rstrip()
            if line:
                print(line)
    except KeyboardInterrupt:
        pass
    finally:
        ser.close()


if __name__ == "__main__":
    main()
