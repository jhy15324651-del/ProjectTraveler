#!/bin/bash
# ================================================
# ProjectTraveler - EC2 배포 스크립트
# ================================================
# EC2 서버에서 실행
# 사용법: bash scripts/deploy.sh
# ================================================

set -e

PROJECT_DIR="/home/ec2-user/ProjectTraveler1"
DOCKER_DIR="${PROJECT_DIR}/docker"

echo "========================================"
echo " ProjectTraveler 배포 시작"
echo " $(date)"
echo "========================================"

# ── Git pull ──────────────────────────────────────
echo "[1/5] 최신 코드 pull..."
cd "${PROJECT_DIR}"
git pull origin master

# ── 기존 앱 컨테이너만 중지 (DB는 유지) ──────────
echo "[2/5] 기존 app 컨테이너 중지..."
cd "${DOCKER_DIR}"
docker compose stop spring-app nginx || true

# ── 이미지 새로 빌드 ──────────────────────────────
echo "[3/5] Docker 이미지 빌드..."
docker compose build --no-cache spring-app

# ── 서비스 재시작 ─────────────────────────────────
echo "[4/5] 서비스 재시작..."
docker compose up -d

# ── 헬스체크 ─────────────────────────────────────
echo "[5/5] 헬스체크 (30초 대기)..."
sleep 30

if docker compose ps | grep -q "spring-app.*Up"; then
    echo "✅ 배포 성공!"
else
    echo "❌ 배포 실패 - 로그 확인:"
    docker compose logs --tail=50 spring-app
    exit 1
fi

# ── 사용하지 않는 이미지 정리 ────────────────────
docker image prune -f

echo "========================================"
echo " 배포 완료: $(date)"
echo "========================================"