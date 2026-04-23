#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_DIR="${LOG_DIR:-$SERVER_DIR/logs}"
STATE_DIR="${LOG_ARCHIVE_STATE_DIR:-$LOG_DIR/.s3-upload-state}"
MANIFEST_DIR="${LOG_ARCHIVE_MANIFEST_DIR:-$LOG_DIR/.s3-manifest}"
MANIFEST_FILE="$MANIFEST_DIR/manifest-$(date -u +%F).jsonl"
AWS_CLI_BIN="${AWS_CLI_BIN:-aws}"
S3_LOG_PREFIX="${S3_LOG_PREFIX:-${OSS_LOG_PREFIX:-}}"
LOG_ARCHIVE_MIN_AGE_MINUTES="${LOG_ARCHIVE_MIN_AGE_MINUTES:-30}"
LOG_DELETE_AFTER_UPLOAD="${LOG_DELETE_AFTER_UPLOAD:-false}"
HOST_TAG="${LOG_ARCHIVE_HOST_TAG:-$(hostname -s 2>/dev/null || hostname)}"

if [[ -z "$S3_LOG_PREFIX" ]]; then
  echo "[archive_logs_to_oss] missing S3_LOG_PREFIX, for example: s3://your-bucket/prod/hotel"
  exit 1
fi

if [[ "$S3_LOG_PREFIX" != s3://* ]]; then
  echo "[archive_logs_to_oss] invalid S3_LOG_PREFIX: $S3_LOG_PREFIX (must start with s3://)"
  exit 1
fi

if ! command -v "$AWS_CLI_BIN" >/dev/null 2>&1; then
  echo "[archive_logs_to_oss] aws cli not found: $AWS_CLI_BIN"
  exit 1
fi

if [[ ! -d "$LOG_DIR" ]]; then
  echo "[archive_logs_to_oss] log directory not found: $LOG_DIR"
  exit 0
fi

mkdir -p "$STATE_DIR" "$MANIFEST_DIR"

is_rotated_log() {
  local name="$1"
  case "$name" in
    *.[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]*.log|*.[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]*.log.gz)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

while IFS= read -r file; do
  base_name="$(basename "$file")"
  if ! is_rotated_log "$base_name"; then
    continue
  fi

  if [[ "$file" != *.gz ]]; then
    gzip -f "$file"
    file="$file.gz"
    base_name="$(basename "$file")"
  fi

  file_hash="$(sha256sum "$file" | awk '{print $1}')"
  marker_file="$STATE_DIR/$file_hash.done"
  if [[ -f "$marker_file" ]]; then
    continue
  fi

  log_date="$(echo "$base_name" | grep -oE '[0-9]{4}-[0-9]{2}-[0-9]{2}' | head -n1 || true)"
  if [[ -z "$log_date" ]]; then
    log_date="$(date -u +%F)"
  fi

  object_path="${S3_LOG_PREFIX%/}/server/$HOST_TAG/$log_date/$base_name"
  "$AWS_CLI_BIN" s3 cp "$file" "$object_path"

  file_size="$(wc -c < "$file" | tr -d ' ')"
  uploaded_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf '{"file":"%s","sha256":"%s","size":%s,"uploaded_at":"%s","object":"%s"}\n' \
    "$base_name" "$file_hash" "$file_size" "$uploaded_at" "$object_path" >> "$MANIFEST_FILE"

  touch "$marker_file"

  if [[ "$LOG_DELETE_AFTER_UPLOAD" == "true" ]]; then
    rm -f "$file"
  fi

done < <(find "$LOG_DIR" -maxdepth 1 -type f \( -name "*.log" -o -name "*.log.gz" \) -mmin "+$LOG_ARCHIVE_MIN_AGE_MINUTES" | sort)

echo "[archive_logs_to_oss] done"
