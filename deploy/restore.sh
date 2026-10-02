#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 2 || "$1" != "--replace-database" ]]; then
  printf 'Usage: bash deploy/restore.sh --replace-database /absolute/path/IPD-backup.sql.gz\n' >&2
  exit 2
fi
restore_file="$2"
[[ -f "$restore_file" ]] || { printf 'Backup file not found.\n' >&2; exit 2; }
gzip -t -- "$restore_file"
deploy_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd -- "$deploy_dir/.." && pwd)"
env_file="${HMS_ENV_FILE:-$deploy_dir/.env}"
compose=(docker compose --env-file "$env_file" -f "$project_dir/compose.yml")
# Save the current database before deliberately replacing its table contents.
bash "$deploy_dir/backup.sh"
"${compose[@]}" stop backend
gzip -dc -- "$restore_file" | "${compose[@]}" exec -T db \
  sh -c 'export MYSQL_PWD="$MYSQL_PASSWORD"; exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'
"${compose[@]}" up -d --wait backend frontend
