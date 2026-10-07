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
| `local` (기본값) | 로컬 개발, 테스트, CI | 테이블 자동 생성(`ddl-auto: update`), Swagger 켜짐 |
| `prod` | 운영 | 스키마 검증만(`ddl-auto: validate`), Swagger 꺼짐 |

운영 프로파일로 실행하려면 `SPRING_PROFILES_ACTIVE=prod`를 지정합니다.

## 환경변수

지정하지 않으면 로컬 Compose의 MySQL 값을 씁니다.

| 이름 | 설명 | 기본값 |
| --- | --- | --- |
| `DB_URL` | JDBC 접속 주소 | `jdbc:mysql://localhost:3306/urijip` |
| `DB_USERNAME` | DB 사용자 | `urijip` |
| `DB_PASSWORD` | DB 비밀번호 | `urijip` |

## 패키지 구조

도메인별로 나누고, 도메인마다 같은 하위 패키지를 둡니다.

```
com.urijip.server
├── member/          # controller, service, repository, entity, dto
├── family/
├── chat/
├── schedule/
└── global/          # config, entity, exception, response
```

호출 방향은 `controller → service → repository`입니다. 이 규칙은 `ArchitectureTest`가 검사합니다.
