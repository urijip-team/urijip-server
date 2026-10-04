# urijip-server

urijip 서비스의 백엔드 서버. 기능 범위는 회원, 가족, 채팅, 일정이다. 이 범위 밖의 기능은 요청 없이 추가하지 않는다.

## 기술 스택

- Java 25, Spring Boot 4.1, Gradle (Groovy DSL, wrapper 사용)
- Spring Web MVC, Spring Data JPA, Validation, Actuator, Lombok
- MySQL 8.4 (로컬은 Docker Compose, 테스트는 Testcontainers)
