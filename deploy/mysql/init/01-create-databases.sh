#!/usr/bin/env bash

# The official MySQL entrypoint sources non-executable .sh initialization
# files. The fallback keeps this script usable if it is made executable.

task004a_fail() {
  echo "TASK-004A initialization failed: $1" >&2
  exit 1
}

task004a_require_identifier() {
  task004a_name="$1"
  task004a_value="$2"

  case "$task004a_value" in
    ""|*[!A-Za-z0-9_]*)
      task004a_fail "$task004a_name must contain only letters, numbers, and underscores"
      ;;
  esac
}

task004a_require_password() {
  task004a_name="$1"
  task004a_value="$2"

  if [ "${#task004a_value}" -lt 20 ]; then
    task004a_fail "$task004a_name must contain at least 20 characters"
  fi

  case "$task004a_value" in
    *[!A-Za-z0-9_@%+=:,./-]*)
      task004a_fail "$task004a_name contains unsupported characters; use a random base64 value"
      ;;
  esac
}

task004a_apply_sql() {
  if command -v docker_process_sql >/dev/null 2>&1; then
    docker_process_sql --database=mysql
  else
    MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket --user=root mysql
  fi
}

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD must be set}"
: "${INFORMATION_HUB_DEV_DATABASE:?INFORMATION_HUB_DEV_DATABASE must be set}"
: "${INFORMATION_HUB_DEV_USER:?INFORMATION_HUB_DEV_USER must be set}"
: "${INFORMATION_HUB_DEV_PASSWORD:?INFORMATION_HUB_DEV_PASSWORD must be set}"
: "${INFORMATION_HUB_TEST_DATABASE:?INFORMATION_HUB_TEST_DATABASE must be set}"
: "${INFORMATION_HUB_TEST_USER:?INFORMATION_HUB_TEST_USER must be set}"
: "${INFORMATION_HUB_TEST_PASSWORD:?INFORMATION_HUB_TEST_PASSWORD must be set}"

task004a_require_identifier "INFORMATION_HUB_DEV_DATABASE" "$INFORMATION_HUB_DEV_DATABASE"
task004a_require_identifier "INFORMATION_HUB_DEV_USER" "$INFORMATION_HUB_DEV_USER"
task004a_require_identifier "INFORMATION_HUB_TEST_DATABASE" "$INFORMATION_HUB_TEST_DATABASE"
task004a_require_identifier "INFORMATION_HUB_TEST_USER" "$INFORMATION_HUB_TEST_USER"
task004a_require_password "INFORMATION_HUB_DEV_PASSWORD" "$INFORMATION_HUB_DEV_PASSWORD"
task004a_require_password "INFORMATION_HUB_TEST_PASSWORD" "$INFORMATION_HUB_TEST_PASSWORD"

if [ "$INFORMATION_HUB_DEV_DATABASE" = "$INFORMATION_HUB_TEST_DATABASE" ]; then
  task004a_fail "development and test databases must be different"
fi

if [ "$INFORMATION_HUB_DEV_USER" = "$INFORMATION_HUB_TEST_USER" ]; then
  task004a_fail "development and test users must be different"
fi

if ! task004a_apply_sql <<TASK004A_SQL
CREATE DATABASE IF NOT EXISTS \`${INFORMATION_HUB_DEV_DATABASE}\`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS \`${INFORMATION_HUB_TEST_DATABASE}\`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS '${INFORMATION_HUB_DEV_USER}'@'%'
  IDENTIFIED BY '${INFORMATION_HUB_DEV_PASSWORD}';
ALTER USER '${INFORMATION_HUB_DEV_USER}'@'%'
  IDENTIFIED BY '${INFORMATION_HUB_DEV_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${INFORMATION_HUB_DEV_DATABASE}\`.* TO '${INFORMATION_HUB_DEV_USER}'@'%';

CREATE USER IF NOT EXISTS '${INFORMATION_HUB_TEST_USER}'@'%'
  IDENTIFIED BY '${INFORMATION_HUB_TEST_PASSWORD}';
ALTER USER '${INFORMATION_HUB_TEST_USER}'@'%'
  IDENTIFIED BY '${INFORMATION_HUB_TEST_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${INFORMATION_HUB_TEST_DATABASE}\`.* TO '${INFORMATION_HUB_TEST_USER}'@'%';

FLUSH PRIVILEGES;
TASK004A_SQL
then
  task004a_fail "database and account SQL failed"
fi

unset task004a_name task004a_value
