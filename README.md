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
