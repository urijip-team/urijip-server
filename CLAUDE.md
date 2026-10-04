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

## Spring Boot 4 주의점

학습 데이터의 상당수가 Boot 3 기준이라 아래를 틀리기 쉽다.

- 테스트 목 객체는 `@MockitoBean`을 쓴다. `@MockBean`은 삭제됐다.
- Jackson은 3 버전이다. 패키지가 `tools.jackson.*`이고, 매퍼를 직접 `@Bean`으로 만들지 말고 `spring.jackson.*` 속성으로 조정한다.
- 퍼시스턴스와 검증은 `jakarta.*` 패키지를 쓴다. `javax.*`는 쓰지 않는다.
- 웹 스타터 이름은 `spring-boot-starter-webmvc`다.

## 데이터베이스

- 접속 정보는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수로 주입하고, 없으면 로컬 Compose 값을 쓴다.
- `ddl-auto: update`는 초기 개발용이다. 운영 배포 전에 마이그레이션 도구로 바꾼다.

## 테스트

- DB가 필요한 테스트는 `@Import(TestcontainersConfiguration.class)`로 실제 MySQL을 쓴다. H2는 쓰지 않는다.
- 단언은 AssertJ(`assertThat`)를 쓴다.

## 패키지 구조

아직 정하지 않았다. 도메인별로 나눌지 계층별로 나눌지 결정되기 전에는 임의로 구조를 만들지 말고 먼저 물어본다.

## 커밋

- 커밋과 푸시는 요청받았을 때만 한다.
- 커밋은 아주 작게 나눈다. 관심사가 다르면 같은 파일이라도 커밋을 나눈다.
- 메시지는 `<type>: <한국어 설명>` 형식이다. type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore` 중 하나.
- `main`에 강제 푸시하지 않는다. 되돌릴 때는 `git revert`를 쓴다.
