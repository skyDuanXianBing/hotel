#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_DIR="${LOG_DIR:-$SERVER_DIR/logs}"
LOG_RETAIN_DAYS="${LOG_RETAIN_DAYS:-3}"
BACKEND_OUT_MAX_MB="${BACKEND_OUT_MAX_MB:-200}"
DRY_RUN="${DRY_RUN:-false}"
STATE_DIR="${LOG_ARCHIVE_STATE_DIR:-$LOG_DIR/.s3-upload-state}"
MANIFEST_DIR="${LOG_ARCHIVE_MANIFEST_DIR:-$LOG_DIR/.s3-manifest}"

if [[ ! -d "$LOG_DIR" ]]; then
  echo "[cleanup_local_logs] log directory not found: $LOG_DIR"
  exit 0
fi

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

remove_file() {
  local target="$1"
  if [[ "$DRY_RUN" == "true" ]]; then
    echo "[cleanup_local_logs] dry-run remove $target"
    return
  fi
  rm -f "$target"
}

while IFS= read -r file; do
  base_name="$(basename "$file")"
  if is_rotated_log "$base_name"; then
    remove_file "$file"
  fi
done < <(find "$LOG_DIR" -maxdepth 1 -type f \( -name "*.log" -o -name "*.log.gz" \) -mtime "+$LOG_RETAIN_DAYS" | sort)

find "$STATE_DIR" -maxdepth 1 -type f -mtime +30 -print0 2>/dev/null | while IFS= read -r -d '' marker; do
  remove_file "$marker"
done

find "$MANIFEST_DIR" -maxdepth 1 -type f -mtime +90 -print0 2>/dev/null | while IFS= read -r -d '' manifest; do
  remove_file "$manifest"
done

# Backward compatibility: clean historical OSS state/manifests if present.
find "$LOG_DIR/.oss-upload-state" -maxdepth 1 -type f -mtime +30 -print0 2>/dev/null | while IFS= read -r -d '' legacy_marker; do
  remove_file "$legacy_marker"
done

find "$LOG_DIR/.oss-manifest" -maxdepth 1 -type f -mtime +90 -print0 2>/dev/null | while IFS= read -r -d '' legacy_manifest; do
  remove_file "$legacy_manifest"
done

BACKEND_OUT="$LOG_DIR/backend.out"
if [[ -f "$BACKEND_OUT" ]]; then
  backend_out_size_bytes="$(stat -c%s "$BACKEND_OUT")"
  backend_out_size_mb="$((backend_out_size_bytes / 1024 / 1024))"
  if (( backend_out_size_mb > BACKEND_OUT_MAX_MB )); then
    if [[ "$DRY_RUN" == "true" ]]; then
      echo "[cleanup_local_logs] dry-run truncate $BACKEND_OUT (${backend_out_size_mb}MB)"
    else
      tail -n 2000 "$BACKEND_OUT" > "$LOG_DIR/backend.out.last2000.log" || true
      : > "$BACKEND_OUT"
      echo "[cleanup_local_logs] truncated $BACKEND_OUT (${backend_out_size_mb}MB)"
    fi
  fi
fi

echo "[cleanup_local_logs] done"
