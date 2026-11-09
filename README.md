# urijip-server

가족 안전 + 소통 앱 '우리집'의 백엔드 서버입니다. 모바일 앱(Flutter)과 관리자 웹(React)이 쓰는 모든 API를 담당합니다.

## 기획서

기능, API, DB 설계는 노션 기획서를 따릅니다.

- [우리집 기능명세서 (초안)](https://app.notion.com/p/3edecd8444f881e89e9cedb53f2f6eaa): 개요, 공통 정책, 기능 목록
- 하위 페이지: 역할별 업무 분담, API 명세서, ERD

## 기술 스택

- Java 25, Spring Boot 4.1, Gradle
- Spring Web MVC, Spring Data JPA, Spring Security, Validation, Actuator
- springdoc-openapi (Swagger UI)
- MySQL 8.4
- 테스트: JUnit 5, AssertJ, Testcontainers, ArchUnit

## 시작하기

준비물은 JDK 25와 Docker입니다. Docker는 실행 중이어야 합니다.

```bash
git clone https://github.com/urijip-team/urijip-server.git
cd urijip-server
./gradlew bootRun
```

`bootRun`을 실행하면 `compose.yaml`의 MySQL 컨테이너가 자동으로 뜹니다. DB를 따로 설치하지 않아도 됩니다.

## API 문서

서버를 띄운 뒤 <http://localhost:8080/swagger-ui.html>에서 Swagger UI를 봅니다. 운영 프로파일에서는 꺼져 있습니다.

## 빌드와 테스트

```bash
./gradlew build   # 컴파일 + 전체 테스트
./gradlew test    # 테스트만
```

테스트는 Testcontainers로 실제 MySQL을 띄우므로 Docker가 필요합니다.

## 프로파일

| 프로파일 | 용도 | 특징 |
| --- | --- | --- |
| `local` (기본값) | 로컬 개발, 테스트, CI | Swagger 켜짐 |
| `prod` | 운영 | Swagger 꺼짐 |

두 프로파일 모두 테이블은 Flyway가 `src/main/resources/db/migration`의 SQL로 만들고, Hibernate는 엔티티와 맞는지 검증만 합니다(`ddl-auto: validate`).

운영 프로파일로 실행하려면 `SPRING_PROFILES_ACTIVE=prod`를 지정합니다.

## 환경변수

로컬(`local`)은 지정하지 않아도 Compose의 MySQL·LocalStack 값을 씁니다. 운영(`prod`)은 아래 값을 줍니다.

| 이름 | 설명 | 운영 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 프로파일 | `prod` (이미지 기본값) |
| `DB_URL` | JDBC 접속 주소 | 필수 |
| `DB_USERNAME` | DB 사용자 | 필수 |
| `DB_PASSWORD` | DB 비밀번호 | 필수 |
| `JWT_SECRET` | JWT 서명 키 (32바이트 이상) | 필수 |
| `AES_KEY` | 계좌번호 암호화 키 (Base64, 32바이트) | 필수. 바꾸면 저장된 계좌번호를 못 읽는다 |
| `S3_BUCKET` | 사진 버킷 (비공개) | 필수 |
| `AWS_REGION` | S3 리전 | 선택, 기본 `ap-northeast-2` |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | S3 자격 증명 | IAM 역할을 쓰면 불필요 |
| `CORS_ALLOWED_ORIGINS` | 관리자 웹 주소(쉼표로 여러 개) | 관리자 웹을 쓰면 필수. 없으면 브라우저 요청을 모두 막는다 |
| `FIREBASE_CREDENTIALS_PATH` | Firebase 서비스 계정 키 파일 경로 | 선택. 없으면 푸시를 보내지 않고 로그만 남긴다 |

## 배포

EC2 + RDS 자동 배포 설정은 [deploy/README.md](deploy/README.md)에 있습니다.

`main`에 머지되면 GitHub Actions(`.github/workflows/image.yml`)가 이미지를 만들어 `ghcr.io/urijip-team/urijip-server`에 올립니다. 태그는 `latest`와 커밋 SHA입니다.

```bash
docker run -d -p 8080:8080 \
  -e DB_URL=jdbc:mysql://<host>:3306/urijip -e DB_USERNAME=... -e DB_PASSWORD=... \
  -e JWT_SECRET=... -e AES_KEY=... -e S3_BUCKET=... \
  -v /secure/firebase.json:/secrets/firebase.json:ro -e FIREBASE_CREDENTIALS_PATH=/secrets/firebase.json \
  ghcr.io/urijip-team/urijip-server:<커밋 SHA>
```

- 테이블은 서버가 뜰 때 Flyway가 만들거나 바꿉니다.
- 상태 확인은 `GET /actuator/health`입니다(인증 없음).
- 이미지는 root가 아닌 사용자로 실행되고, JVM 메모리는 컨테이너 메모리의 75%까지 씁니다.

## 패키지 구조

도메인별로 나누고, 도메인마다 같은 하위 패키지를 둡니다.

```
com.urijip.server
├── member/          # controller, service, repository, entity, dto
├── family/
├── chat/
├── schedule/
└── global/          # config, entity, exception, response, security
```

호출 방향은 `controller → service → repository`입니다. 이 규칙은 `ArchitectureTest`가 검사합니다.

## 작업 방식

- `main`에 직접 커밋하지 않습니다. 작업마다 `<type>/<짧은-영문-이름>` 브랜치를 만들고 PR로 합칩니다.
- 커밋 메시지와 PR 제목은 `<type>: <한국어 설명>` 형식입니다. type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore` 중 하나입니다.
- PR을 올리면 CI가 `./gradlew build`를 돌립니다. 통과해야 합칩니다.
