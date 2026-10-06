# urijip-server

가족 안전 + 소통 앱 '우리집'의 백엔드 서버. 모바일 앱(Flutter)과 관리자 웹(React)이 쓰는 모든 API를 담당한다.

기능 범위는 노션 기획서 전체다: 인증·회원, 가족 그룹, 프로필, 위치 공유, SOS, 채팅, 캘린더(일정·할 일), 사진 앨범, 용돈 요청, 신고·문의, 공지, 관리자 API. 기획서에 없는 기능은 요청 없이 추가하지 않는다.

## 기획서

기획서는 노션에 있다. 기능을 만들기 전에 해당 부분을 읽고 따른다.

- [우리집 기능명세서 (초안)](https://app.notion.com/p/3edecd8444f881e89e9cedb53f2f6eaa): 개요, 공통 정책, 기능 목록(기능 ID·우선순위)
- 하위 페이지: 역할별 업무 분담, API 명세서, ERD

URL, 요청·응답 필드, 테이블·컬럼 이름은 API 명세서와 ERD를 따른다. 예를 들어 `member` 패키지의 테이블은 `users`, 경로는 `/api/users`다. 기획서와 다르게 만들어야 하면 먼저 묻는다.

## 기술 스택

- Java 25, Spring Boot 4.1, Gradle (Groovy DSL, wrapper 사용)
- Spring Web MVC, Spring Data JPA, Spring Security, Validation, Actuator, Lombok
- springdoc-openapi (Swagger UI)
- MySQL 8.4 (로컬은 Docker Compose, 테스트는 Testcontainers)

## 명령

```bash
./gradlew build      # 컴파일 + 전체 테스트. 작업 완료 전에 반드시 통과시킨다
./gradlew test       # 테스트만
./gradlew bootRun    # 로컬 실행. compose.yaml의 MySQL이 자동으로 뜬다
./gradlew bootRun --args='--spring.profiles.active=prod'   # 운영 프로파일로 실행
```

테스트와 `bootRun` 모두 Docker가 실행 중이어야 한다. 로컬 실행 후 `http://localhost:8080/swagger-ui.html`에서 API 문서를 본다.

## Spring Boot 4 주의점

학습 데이터의 상당수가 Boot 3 기준이라 아래를 틀리기 쉽다.

- 테스트 목 객체는 `@MockitoBean`을 쓴다. `@MockBean`은 삭제됐다.
- Jackson은 3 버전이다. 패키지가 `tools.jackson.*`이고, 매퍼를 직접 `@Bean`으로 만들지 말고 `spring.jackson.*` 속성으로 조정한다.
- 퍼시스턴스와 검증은 `jakarta.*` 패키지를 쓴다. `javax.*`는 쓰지 않는다.
- 웹 스타터 이름은 `spring-boot-starter-webmvc`다.
- springdoc은 3.x가 Boot 4용이다. 2.x는 쓰지 않는다.

## API 응답

모든 응답은 `global/response/ApiResponse`로 감싼다. API 명세서의 공통 형식이다.

```json
{ "success": true, "data": { }, "error": null }
{ "success": false, "data": null, "error": { "code": "FAMILY_NOT_MEMBER", "message": "해당 가족의 멤버가 아닙니다." } }
```

- 컨트롤러는 `ApiResponse.success(data)`를 반환한다.
- 실패는 서비스에서 `new BusinessException(ErrorCode.X)`를 던진다. `GlobalExceptionHandler`가 응답으로 바꾼다. 컨트롤러에서 `try-catch`로 에러 응답을 만들지 않는다.
- 새 에러는 `ErrorCode`에 추가한다. 이름과 HTTP 상태는 API 명세서의 에러 코드 표를 따른다.

## 설정과 프로파일

- `application.yaml`은 공통 설정이다. 환경마다 달라지는 값은 `application-local.yaml`, `application-prod.yaml`에 둔다.
- 프로파일을 지정하지 않으면 `local`이다. 테스트와 CI도 `local`로 돈다.
- `prod`는 `ddl-auto: validate`이고 Swagger를 끈다. 운영 서버는 `SPRING_PROFILES_ACTIVE=prod`로 실행한다.
- 설정 클래스는 `global/config`에 관심사별로 하나씩 둔다. `@EnableJpaAuditing` 같은 `@Enable...`을 `UrijipServerApplication`에 붙이지 않는다. `@WebMvcTest`가 깨진다.

## 데이터베이스

- 접속 정보는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수로 주입하고, 없으면 로컬 Compose 값을 쓴다.
- `local`의 `ddl-auto: update`는 초기 개발용이다. 운영 배포 전에 마이그레이션 도구로 바꾼다.
- `created_at`이 있는 엔티티는 `global/entity/BaseTimeEntity`를 상속한다. 시간을 직접 넣지 않는다.

## 시간

- 서버 기본 시간대는 `Asia/Seoul`이다. `UrijipServerApplication.main`에서 고정한다.
- 엔티티의 시간은 `LocalDateTime`이다. 응답 날짜는 ISO-8601이고 Jackson 기본값이 이미 그렇다.
- API 명세서의 `+09:00` 오프셋은 `LocalDateTime`으로는 붙지 않는다. 응답 DTO에서 `OffsetDateTime`으로 바꿔 내보낸다.

## 보안

- `global/config/SecurityConfig`는 지금 모든 요청을 허용하는 뼈대다. JWT 필터와 경로별 규칙은 로그인 기능을 만들 때 붙인다.
- 인증은 JWT(Access 30분, Refresh 14일), 비밀번호는 BCrypt다. 앱 API는 `/api/...`, 관리자 API는 `/admin/...`(ADMIN만)이다.
- 가족에 속한 데이터는 같은 가족 멤버만 조회·수정할 수 있다. `/api/families/{familyId}/...`는 요청자가 그 가족의 멤버인지 검사한다.

## 테스트

- DB가 필요한 테스트는 `@Import(TestcontainersConfiguration.class)`로 실제 MySQL을 쓴다. H2는 쓰지 않는다.
- `@WebMvcTest`는 `@Configuration` 클래스를 읽지 않는다. 컨트롤러 테스트에는 `SecurityConfig`를 `@Import`한다. 빠뜨리면 모든 요청이 401로 막힌다.
- 단언은 AssertJ(`assertThat`)를 쓴다. MockMvc는 `MockMvcTester`를 쓴다.

## 패키지 구조

도메인별로 나눈다.

```
com.urijip.server
├── member/          # 도메인마다 같은 하위 구조
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
├── family/
├── chat/
├── schedule/
└── global/          # 도메인에 속하지 않는 것
    ├── config/      # 설정 클래스
    ├── entity/      # BaseTimeEntity
    ├── exception/   # ErrorCode, BusinessException, GlobalExceptionHandler
    └── response/    # ApiResponse
```

위치, SOS, 사진, 용돈 등 나머지 도메인도 만들 때 같은 구조로 패키지를 추가한다.

- 호출 방향은 `controller → service → repository`다. 컨트롤러는 리포지토리를 직접 쓰지 않는다.
- 다른 도메인의 데이터가 필요하면 그 도메인의 `service`를 호출한다. 다른 도메인의 `repository`는 쓰지 않는다.
- `global`은 도메인 패키지에 의존하지 않는다.
- 엔티티를 API 응답으로 바로 내보내지 않고 `dto`로 변환한다.

이 규칙은 `ArchitectureTest`(ArchUnit)가 검사한다. 새 도메인은 위 다섯 하위 패키지를 그대로 따른다.

## 커밋

- 커밋과 푸시는 요청받았을 때만 한다.
- 커밋은 아주 작게 나눈다. 관심사가 다르면 같은 파일이라도 커밋을 나눈다.
- 메시지는 `<type>: <한국어 설명>` 형식이다. type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore` 중 하나.
- `main`에 강제 푸시하지 않는다. 되돌릴 때는 `git revert`를 쓴다.

## 스킬

`.claude/skills/`의 스킬 중 일부는 외부 오픈소스를 가져와 고친 것이다. 출처와 라이선스는 `.claude/skills/NOTICE.md`에 있으며, 외부 스킬을 추가하면 여기에도 기록한다.
