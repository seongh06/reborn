#!/bin/bash
set -euo pipefail

# ReBorn 서버 수동 배포 스크립트
# 사용법: ./deploy.sh

PROJECT_ROOT="/home/seongh06/reborn" # 실제 서버 경로에 맞춰 수정
COMPOSE_FILE="server/deployment/docker/docker-compose.yml"

echo "🚀 [ReBorn] 배포를 시작합니다..."

cd "$PROJECT_ROOT"

echo "📥 1. 최신 소스 코드 가져오기 (git pull)..."
git pull origin dev

echo "🏗️ 2. 도커 컨테이너 빌드 및 실행..."
# --build: 소스 변경 시 이미지 새로 생성
# -d: 백그라운드 실행
docker compose -f "$COMPOSE_FILE" --project-directory . up -d --build

echo "🧹 3. 사용하지 않는 이미지 정리 (Prune)..."
docker image prune -f

echo "✅ 배포가 완료되었습니다!"
docker compose -f "$COMPOSE_FILE" --project-directory . ps
