#!/usr/bin/env bash
set -euo pipefail

mysql_container="${MYSQL_CONTAINER:-helidon-stored-procedures-mysql}"
mysql_database="${MYSQL_DATABASE:-order_processing}"
mysql_user="${MYSQL_USER:-user}"
mysql_password="${MYSQL_PASSWORD:-changeit}"

if docker container inspect "${mysql_container}" >/dev/null 2>&1; then
    container_command="$(docker container inspect "${mysql_container}" --format '{{join .Config.Cmd " "}}')"
    container_environment="$(docker container inspect "${mysql_container}" \
        --format '{{range .Config.Env}}{{println .}}{{end}}')"
    if [[ "${container_command}" != *"--skip-log-bin"* ]] \
            || [[ "${container_environment}" != *"MYSQL_RANDOM_ROOT_PASSWORD=yes"* ]]; then
        echo "Container ${mysql_container} was created with an incompatible sample-database configuration." >&2
        echo "This disposable example requires a fresh container without binary logging so its application account can" >&2
        echo "create the sample stored function. Remove it and rerun this script:" >&2
        echo "docker rm -f ${mysql_container}" >&2
        exit 1
    fi
    docker start "${mysql_container}" >/dev/null 2>&1 || true
else
    docker run --name "${mysql_container}" \
        -p "${MYSQL_PORT:-3306}:3306" \
        -e "MYSQL_DATABASE=${mysql_database}" \
        -e "MYSQL_USER=${mysql_user}" \
        -e "MYSQL_PASSWORD=${mysql_password}" \
        -e MYSQL_RANDOM_ROOT_PASSWORD=yes \
        -d container-registry.oracle.com/mysql/community-server:8.4.9-aarch64 --skip-log-bin >/dev/null
fi

until docker exec "${mysql_container}" mysql \
        -u"${mysql_user}" -p"${mysql_password}" \
        --skip-column-names -e "SELECT 1" >/dev/null 2>&1; do
    sleep 1
done

echo "MySQL is ready in container ${mysql_container}"
