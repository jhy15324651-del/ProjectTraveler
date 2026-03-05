#!/bin/bash
# ================================================
# ProjectTraveler - DB 백업 스크립트
# ================================================
# EC2 서버에서 실행
# 사용법: bash scripts/backup.sh
#
# 자동화: crontab -e
#   0 2 * * * /home/ec2-user/ProjectTraveler1/scripts/backup.sh
# ================================================

set -e

# ── 설정 ─────────────────────────────────────────
BACKUP_DIR="/home/ec2-user/backups/mariadb"
CONTAINER_NAME="traveler-db"
DB_NAME="lms_local"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/lms_local_${DATE}.sql.gz"
KEEP_DAYS=7   # 7일치 보관

# ── 백업 디렉토리 생성 ────────────────────────────
mkdir -p "${BACKUP_DIR}"

echo "[$(date)] DB 백업 시작: ${DB_NAME}"

# ── 백업 실행 (컨테이너 내부 mysqldump) ──────────
docker exec "${CONTAINER_NAME}" \
    mysqldump \
    -u root \
    -p"${DB_ROOT_PASSWORD}" \
    --single-transaction \
    --routines \
    --triggers \
    "${DB_NAME}" \
    | gzip > "${BACKUP_FILE}"

echo "[$(date)] 백업 완료: ${BACKUP_FILE}"
echo "[$(date)] 파일 크기: $(du -sh ${BACKUP_FILE} | cut -f1)"

# ── 오래된 백업 삭제 (7일 이상) ──────────────────
find "${BACKUP_DIR}" -name "*.sql.gz" -mtime "+${KEEP_DAYS}" -delete
echo "[$(date)] ${KEEP_DAYS}일 이상 된 백업 정리 완료"

echo "[$(date)] 백업 작업 종료"