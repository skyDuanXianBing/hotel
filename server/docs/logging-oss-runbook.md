# Logging + S3 Runbook

This runbook implements a hot-local + cold-archive strategy:
- Keep short-term logs locally for fast troubleshooting
- Upload rotated logs to S3 for historical search
- Prevent `backend.out` from growing without limit

## 1. Environment Variables

Add these variables in `server/.env`:

```env
# Logging volume controls
LOG_PATH=/home/ec2-user/hotel/server/logs
LOG_MAX_FILE_SIZE=100MB
LOG_MAX_HISTORY_DAYS=7
LOG_TOTAL_SIZE_CAP=2GB
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false

# S3 archive controls
S3_LOG_PREFIX=s3://your-bucket/prod/hotel
AWS_CLI_BIN=aws
LOG_ARCHIVE_MIN_AGE_MINUTES=30
LOG_DELETE_AFTER_UPLOAD=false
LOG_ARCHIVE_HOST_TAG=hotel-prod-a

# Local cleanup controls
LOG_RETAIN_DAYS=3
BACKEND_OUT_MAX_MB=200
```

## 2. Startup Mode

Use systemd and avoid shell redirection like `> backend.out 2>&1`.

Reference file:
- `server/deploy/systemd/hotel-server.service.example`

## 3. IAM / Credentials

Use one of these options before enabling archive:
- Recommended (EC2): attach an IAM Role to the instance, grant bucket write/list permissions.
- Alternative: configure `~/.aws/credentials` (Access Key), and set `AWS_PROFILE` if needed.

Quick checks:

```bash
aws sts get-caller-identity
aws s3 ls s3://your-bucket/prod/hotel/
```

## 4. Enable Scheduled Jobs

Reference cron file:
- `server/deploy/cron/log-maintenance.cron.example`

Install steps:

```bash
cd /home/ec2-user/hotel/server
chmod +x ./scripts/logs/archive_logs_to_oss.sh
chmod +x ./scripts/logs/cleanup_local_logs.sh

crontab -l > /tmp/current.cron || true
cat ./deploy/cron/log-maintenance.cron.example >> /tmp/current.cron
crontab /tmp/current.cron
rm -f /tmp/current.cron
```

## 5. Manual Commands

Archive once:

```bash
cd /home/ec2-user/hotel/server
./scripts/logs/archive_logs_to_oss.sh
```

Cleanup once:

```bash
cd /home/ec2-user/hotel/server
./scripts/logs/cleanup_local_logs.sh
```

Dry-run cleanup:

```bash
cd /home/ec2-user/hotel/server
DRY_RUN=true ./scripts/logs/cleanup_local_logs.sh
```

## 6. What Gets Uploaded

Only rotated logs are uploaded (files with date pattern in filename), for example:
- `application.2026-04-23.0.log`
- `error.2026-04-23.1.log.gz`
- `su-reservation.2026-04-23.0.log`

Active files like `application.log` are not uploaded.

## 7. Troubleshooting

1. Check archive logs:
   - `server/logs/log-archive-cron.log`
2. Check cleanup logs:
   - `server/logs/log-cleanup-cron.log`
3. Check manifest files:
   - `server/logs/.s3-manifest/manifest-YYYY-MM-DD.jsonl`
4. If upload fails, verify AWS authentication (`aws sts get-caller-identity`) and S3 bucket permissions.

## 8. Rollback

If you need to pause S3 archive quickly:
1. Comment out the archive cron line
2. Keep cleanup cron enabled
3. Keep application file rolling enabled (already configured in logback)
