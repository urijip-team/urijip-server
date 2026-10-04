# urijip-server

urijip 서비스의 백엔드 서버. 기능 범위는 회원, 가족, 채팅, 일정이다. 이 범위 밖의 기능은 요청 없이 추가하지 않는다.

## 기술 스택

- Java 25, Spring Boot 4.1, Gradle (Groovy DSL, wrapper 사용)
- Spring Web MVC, Spring Data JPA, Validation, Actuator, Lombok
- MySQL 8.4 (로컬은 Docker Compose, 테스트는 Testcontainers)

## 명령

```bash
./gradlew build      # 컴파일 + 전체 테스트. 작업 완료 전에 반드시 통과시킨다
./gradlew test       # 테스트만
./gradlew bootRun    # 로컬 실행. compose.yaml의 MySQL이 자동으로 뜬다
```

테스트와 `bootRun` 모두 Docker가 실행 중이어야 한다.
