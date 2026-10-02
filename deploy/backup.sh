#!/usr/bin/env bash
set -euo pipefail
deploy_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd -- "$deploy_dir/.." && pwd)"
env_file="${HMS_ENV_FILE:-$deploy_dir/.env}"
backup_dir="${HMS_BACKUP_DIR:-$deploy_dir/backups}"
umask 077
mkdir -p -- "$backup_dir"
backup_file="$backup_dir/IPD-$(date -u +%Y%m%dT%H%M%S)-$$.sql.gz"
partial_file="$backup_file.partial"
trap 'rm -f -- "$partial_file"' EXIT
docker compose --env-file "$env_file" -f "$project_dir/compose.yml" exec -T db \
  sh -c 'export MYSQL_PWD="$MYSQL_PASSWORD"; exec mysqldump -u"$MYSQL_USER" --single-transaction --no-tablespaces --triggers "$MYSQL_DATABASE"' \
  | gzip > "$partial_file"
gzip -t -- "$partial_file"
mv -- "$partial_file" "$backup_file"
printf '%s\n' "$backup_file"
