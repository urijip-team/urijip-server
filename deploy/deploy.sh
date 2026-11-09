#!/usr/bin/env bash
# EC2에서 새 이미지로 서버를 바꿔 띄운다. 상태 확인이 실패하면 이전 이미지로 되돌린다.
#
# 사용법: deploy.sh <이미지>   예: deploy.sh ghcr.io/urijip-team/urijip-server:<커밋 SHA>
# 환경변수(선택):
#   CONFIG_DIR  운영 설정 폴더. urijip.env(환경변수)와 firebase.json을 둔다. 기본 /etc/urijip
#   CONTAINER   컨테이너 이름. 기본 urijip-server
#   PORT        호스트 포트. 기본 8080
#   SKIP_PULL   1이면 이미지를 받지 않는다(이미 있는 이미지로 시험할 때)
set -euo pipefail

IMAGE=${1:?사용법: deploy.sh <이미지>}
CONFIG_DIR=${CONFIG_DIR:-/etc/urijip}
NAME=${CONTAINER:-urijip-server}
PORT=${PORT:-8080}
HEALTH_URL="http://localhost:${PORT}/actuator/health"

if [ "${SKIP_PULL:-0}" != 1 ]; then
	docker pull "$IMAGE"
fi

PREVIOUS=$(docker inspect --format '{{.Config.Image}}' "$NAME" 2>/dev/null || true)

start() {
	docker rm -f "$NAME" >/dev/null 2>&1 || true
	# firebase.json은 컨테이너의 /secrets/firebase.json으로 보인다(FIREBASE_CREDENTIALS_PATH=/secrets/firebase.json).
	docker run -d --name "$NAME" --restart unless-stopped -p "${PORT}:8080" \
		--env-file "${CONFIG_DIR}/urijip.env" -v "${CONFIG_DIR}:/secrets:ro" "$1" >/dev/null
}

healthy() {
	for _ in $(seq 1 60); do
		if curl -fsS "$HEALTH_URL" 2>/dev/null | grep -q '"UP"'; then
			return 0
		fi
		sleep 2
	done
	return 1
}

start "$IMAGE"
if healthy; then
	echo "배포 완료: $IMAGE"
	docker image prune -f >/dev/null
	exit 0
fi

echo "상태 확인 실패: $IMAGE" >&2
docker logs --tail 50 "$NAME" >&2 || true
if [ -n "$PREVIOUS" ] && [ "$PREVIOUS" != "$IMAGE" ]; then
	start "$PREVIOUS"
	if healthy; then
		echo "이전 이미지로 되돌림: $PREVIOUS" >&2
	else
		echo "이전 이미지도 뜨지 않음: $PREVIOUS" >&2
	fi
fi
exit 1
