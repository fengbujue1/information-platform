#!/usr/bin/env bash

set -Eeuo pipefail

task004a_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
task004a_deploy_dir="$(cd "$task004a_script_dir/../.." && pwd)"
cd "$task004a_deploy_dir"

if [ ! -f .env ]; then
  echo "TASK-004A verification failed: create deploy/.env first" >&2
  exit 1
fi

task004a_compose() {
  docker compose --env-file .env --file docker-compose.yml "$@"
}

task004a_wait_for_health() {
  task004a_attempt=0

  while [ "$task004a_attempt" -lt 90 ]; do
    task004a_container_id="$(task004a_compose ps --quiet mysql)"
    if [ -n "$task004a_container_id" ]; then
      task004a_health="$(
        docker inspect \
          --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' \
          "$task004a_container_id"
      )"

      if [ "$task004a_health" = "healthy" ]; then
        return 0
      fi
    fi

    task004a_attempt=$((task004a_attempt + 1))
    sleep 2
  done

  echo "TASK-004A verification failed: MySQL did not become healthy" >&2
  task004a_compose ps >&2
  return 1
}

task004a_root_mysql() {
  task004a_compose exec --no-TTY mysql sh -c \
    'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --batch --skip-column-names --user=root "$@"' \
    task004a-root "$@"
}

task004a_dev_mysql() {
  task004a_compose exec --no-TTY mysql sh -c \
    'MYSQL_PWD="$INFORMATION_HUB_DEV_PASSWORD" exec mysql --batch --skip-column-names --user="$INFORMATION_HUB_DEV_USER" "$INFORMATION_HUB_DEV_DATABASE" "$@"' \
    task004a-dev "$@"
}

task004a_test_mysql() {
  task004a_compose exec --no-TTY mysql sh -c \
    'MYSQL_PWD="$INFORMATION_HUB_TEST_PASSWORD" exec mysql --batch --skip-column-names --user="$INFORMATION_HUB_TEST_USER" "$INFORMATION_HUB_TEST_DATABASE" "$@"' \
    task004a-test "$@"
}

task004a_probe_table="task_004a_persistence_probe"
task004a_backup_dir="$task004a_deploy_dir/mysql/backups"
task004a_backup_file="$task004a_backup_dir/task-004a-probe.sql"
task004a_probe_created=0

task004a_cleanup() {
  set +e

  if [ "$task004a_probe_created" -eq 1 ]; then
    task004a_test_mysql \
      --execute="DROP TABLE IF EXISTS ${task004a_probe_table};" \
      >/dev/null 2>&1
  fi

  rm -f "$task004a_backup_file"
}

trap task004a_cleanup EXIT

task004a_compose config --quiet
task004a_compose up --detach
task004a_wait_for_health

task004a_version="$(task004a_root_mysql --execute='SELECT VERSION();')"
case "$task004a_version" in
  8.4.10*)
    ;;
  *)
    echo "TASK-004A verification failed: expected MySQL 8.4.10, got $task004a_version" >&2
    exit 1
    ;;
esac

task004a_dev_mysql --execute='SELECT 1;' >/dev/null
task004a_test_mysql --execute='SELECT 1;' >/dev/null

if task004a_compose exec --no-TTY mysql sh -c \
  'MYSQL_PWD="$INFORMATION_HUB_DEV_PASSWORD" exec mysql --user="$INFORMATION_HUB_DEV_USER" "$INFORMATION_HUB_TEST_DATABASE" --execute="SELECT 1;"' \
  >/dev/null 2>&1; then
  echo "TASK-004A verification failed: development user can access test database" >&2
  exit 1
fi

if task004a_compose exec --no-TTY mysql sh -c \
  'MYSQL_PWD="$INFORMATION_HUB_TEST_PASSWORD" exec mysql --user="$INFORMATION_HUB_TEST_USER" "$INFORMATION_HUB_DEV_DATABASE" --execute="SELECT 1;"' \
  >/dev/null 2>&1; then
  echo "TASK-004A verification failed: test user can access development database" >&2
  exit 1
fi

for task004a_business_table in \
  information_item \
  job_information \
  information_snapshot; do
  task004a_dev_count="$(
    task004a_dev_mysql \
      --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '${task004a_business_table}';"
  )"
  task004a_test_count="$(
    task004a_test_mysql \
      --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '${task004a_business_table}';"
  )"

  if [ "$task004a_dev_count" -ne 0 ] || [ "$task004a_test_count" -ne 0 ]; then
    echo "TASK-004A verification failed: business table $task004a_business_table already exists" >&2
    exit 1
  fi
done

task004a_test_mysql --execute="
  CREATE TABLE IF NOT EXISTS ${task004a_probe_table} (
    probe_id BIGINT NOT NULL PRIMARY KEY,
    probe_value VARCHAR(64) NOT NULL
  ) ENGINE=InnoDB;
  START TRANSACTION;
  DELETE FROM ${task004a_probe_table};
  INSERT INTO ${task004a_probe_table} (probe_id, probe_value)
  VALUES (1, 'task-004a-persistence-ok');
  COMMIT;
"
task004a_probe_created=1

task004a_container_id="$(task004a_compose ps --quiet mysql)"
if ! docker inspect \
  --format '{{range .Mounts}}{{println .Destination}}{{end}}' \
  "$task004a_container_id" |
  grep --fixed-strings --line-regexp '/var/lib/mysql' >/dev/null; then
  echo "TASK-004A verification failed: /var/lib/mysql is not mounted" >&2
  exit 1
fi

task004a_compose down
task004a_compose up --detach
task004a_wait_for_health

task004a_probe_value="$(
  task004a_test_mysql \
    --execute="SELECT probe_value FROM ${task004a_probe_table} WHERE probe_id = 1;"
)"
if [ "$task004a_probe_value" != "task-004a-persistence-ok" ]; then
  echo "TASK-004A verification failed: persistence probe was not preserved" >&2
  exit 1
fi

mkdir -p "$task004a_backup_dir"
chmod 700 "$task004a_backup_dir"

task004a_compose exec --no-TTY mysql sh -c \
  'MYSQL_PWD="$INFORMATION_HUB_TEST_PASSWORD" exec mysqldump --single-transaction --skip-lock-tables --no-tablespaces --user="$INFORMATION_HUB_TEST_USER" "$INFORMATION_HUB_TEST_DATABASE" task_004a_persistence_probe' \
  >"$task004a_backup_file"

task004a_test_mysql --execute="DROP TABLE ${task004a_probe_table};"
task004a_probe_created=0

task004a_compose exec --no-TTY mysql sh -c \
  'MYSQL_PWD="$INFORMATION_HUB_TEST_PASSWORD" exec mysql --user="$INFORMATION_HUB_TEST_USER" "$INFORMATION_HUB_TEST_DATABASE"' \
  <"$task004a_backup_file"
task004a_probe_created=1

task004a_restored_value="$(
  task004a_test_mysql \
    --execute="SELECT probe_value FROM ${task004a_probe_table} WHERE probe_id = 1;"
)"
if [ "$task004a_restored_value" != "task-004a-persistence-ok" ]; then
  echo "TASK-004A verification failed: backup restore probe was not restored" >&2
  exit 1
fi

echo "TASK-004A remote MySQL verification passed."
