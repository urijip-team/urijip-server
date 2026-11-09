# EC2 + RDS 배포

`main`에 머지되면 Image 워크플로가 이미지를 GHCR에 올리고, 성공하면 Deploy 워크플로가 EC2에 SSH로 접속해 `deploy/deploy.sh`로 그 커밋의 이미지를 띄웁니다. 새 서버가 `/actuator/health`에서 UP이 되지 않으면 이전 이미지로 되돌립니다.

처음 한 번 아래를 준비하고, 저장소 변수 `DEPLOY_ENABLED`를 `true`로 바꾸면 자동 배포가 켜집니다.

## 1. RDS (MySQL)

- 엔진 MySQL 8.4, DB 이름 `urijip`, 문자셋은 기본값(utf8mb4)
- 보안 그룹: EC2의 보안 그룹에서 오는 3306만 허용
- 테이블은 서버가 뜰 때 Flyway가 만듭니다. 빈 DB로 시작하면 됩니다.

## 2. S3와 IAM 역할

- 사진 버킷은 비공개(퍼블릭 액세스 차단)로 만듭니다.
- EC2에 붙일 IAM 역할에 그 버킷의 `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`를 줍니다. 역할을 쓰면 `AWS_ACCESS_KEY_ID`는 필요 없습니다.

## 3. EC2

- Amazon Linux 2023, 메모리 2GB 이상(t3.small 등), 위 IAM 역할 연결
- Docker 설치: `sudo dnf install -y docker && sudo systemctl enable --now docker && sudo usermod -aG docker ec2-user`
- 보안 그룹: 22(SSH, GitHub Actions에서 접속), 8080(로드 밸런서나 리버스 프록시에서만)
- 운영 설정 폴더 `/etc/urijip`(권한 700, 파일 600):

```bash
sudo install -d -m 700 -o ec2-user /etc/urijip
# /etc/urijip/urijip.env
DB_URL=jdbc:mysql://<RDS 엔드포인트>:3306/urijip
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...            # 32바이트 이상
AES_KEY=...               # Base64 32바이트. 바꾸면 저장된 계좌번호를 못 읽는다
S3_BUCKET=...
CORS_ALLOWED_ORIGINS=https://admin.<도메인>
FIREBASE_CREDENTIALS_PATH=/secrets/firebase.json
# /etc/urijip/firebase.json  ← Firebase 서비스 계정 키
```

`/etc/urijip`은 컨테이너 안의 `/secrets`로 읽기 전용으로 보입니다.

## 4. HTTPS

앱과 관리자 웹은 HTTPS로 붙어야 합니다. ALB(ACM 인증서) → EC2 8080이 가장 간단합니다.

- 대상 그룹 상태 확인 경로: `/actuator/health`
- WebSocket(`/ws`)은 ALB가 그대로 넘깁니다. 유휴 제한 시간은 기본 60초면 충분합니다(STOMP 하트비트 10초).

## 5. GitHub

저장소 Settings에서:

| 종류 | 이름 | 값 |
| --- | --- | --- |
| Environment | `production` | 배포를 승인받고 싶으면 Required reviewers 지정 |
| Secret (production) | `EC2_HOST` | EC2 주소 |
| Secret (production) | `EC2_USER` | `ec2-user` |
| Secret (production) | `EC2_SSH_KEY` | 배포용 SSH 개인 키 (EC2의 `~/.ssh/authorized_keys`에 공개 키 등록) |
| Secret (production) | `EC2_KNOWN_HOSTS` | `ssh-keyscan -H <EC2 주소>` 결과 |
| Variable | `DEPLOY_ENABLED` | `true` |

## 수동 배포와 되돌리기

Actions → Deploy → Run workflow에서 `tag`에 커밋 SHA를 넣으면 그 버전으로 배포합니다. 비우면 `latest`입니다. 되돌릴 때는 이전 커밋 SHA를 넣습니다.

서버에서 직접 할 때(저장소의 `deploy/deploy.sh`를 서버에 복사해 둔 뒤):

```bash
echo <GitHub 토큰> | docker login ghcr.io -u <GitHub 아이디> --password-stdin
bash deploy.sh ghcr.io/urijip-team/urijip-server:<커밋 SHA>
```
