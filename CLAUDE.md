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
- AWS S3 (사진 저장. 로컬과 테스트는 LocalStack)

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
- `prod`는 Swagger를 끈다. 운영 서버는 `SPRING_PROFILES_ACTIVE=prod`로 실행한다.
- 운영은 `Dockerfile`로 만든 이미지로 띄운다(기본 프로파일 `prod`). `main`에 머지되면 `.github/workflows/image.yml`이 `ghcr.io/urijip-team/urijip-server`에 `latest`·커밋 SHA 태그로 올린다. 운영 환경변수를 추가하면 README의 환경변수 표도 고친다.
- 설정 클래스는 `global/config`에 관심사별로 하나씩 둔다. `@EnableJpaAuditing` 같은 `@Enable...`을 `UrijipServerApplication`에 붙이지 않는다. `@WebMvcTest`가 깨진다.

## 데이터베이스

- 접속 정보는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수로 주입하고, 없으면 로컬 Compose 값을 쓴다.
- 스키마는 Flyway가 `src/main/resources/db/migration`의 SQL로 만든다. 모든 환경에서 Hibernate는 `ddl-auto: validate`로 엔티티와 맞는지만 본다. 테스트도 빈 MySQL에 마이그레이션을 적용한 뒤 검증하므로, SQL을 빠뜨리면 테스트가 실패한다.
- 엔티티를 바꾸면 같은 커밋에 `V{다음 번호}__{설명}.sql`을 추가한다. 이미 머지된 마이그레이션 파일은 고치지 않는다(적용된 DB와 체크섬이 달라져 서버가 뜨지 않는다).
- enum은 MySQL `enum` 컬럼이다. 값을 추가하면 `ALTER TABLE ... MODIFY ...`로 컬럼 정의도 바꾼다.
- Flyway 전에 Hibernate가 테이블을 만든 DB는 `baseline-on-migrate`로 V1을 적용한 것으로 보고 V2부터 적용한다.
- `bootRun`은 `spring-boot-docker-compose`가 `DB_URL`을 무시하고 compose의 `urijip` DB에 연결한다. 다른 DB를 쓰려면 `--spring.docker.compose.enabled=false`를 함께 준다.
- `created_at`이 있는 엔티티는 `global/entity/BaseTimeEntity`를 상속한다. 시간을 직접 넣지 않는다.
- 가족에 딸린 테이블(`family_id` FK)을 만들면 `FamilyDeletedEvent`를 받아 그 가족의 행을 지운다. 마지막 멤버가 나가 가족이 삭제될 때 FK 때문에 실패하지 않게 하기 위해서다. 예: `ScheduleService.onFamilyDeleted`.
- 종류에 따라 다른 테이블을 가리키는 id(예: `reports.target_id`, `target_type`으로 구분)에는 FK를 두지 않는다. 대상이 지워져도 신고 같은 기록은 남고, 그런 행을 쓰는 쪽에서 대상이 없을 수 있다고 보고 처리한다.
- 행이 많을 수 있는 테이블은 `@Modifying` JPQL로 한 번에 지운다. 예: `ChatMessageRepository.deleteByFamilyId`. 벌크 삭제는 영속성 컨텍스트를 거치지 않아서, 같은 트랜잭션에서 읽어 둔 엔티티가 지워진 행을 가리킨 채 남는다. 테스트에서 저장과 삭제를 한 트랜잭션에 몰아 쓰면 flush 때 실패하므로, 그 사이에 `EntityManager`의 `flush()`·`clear()`로 트랜잭션 경계를 흉내 낸다.

## 파일 저장 (S3)

- 사진 같은 파일은 `global/storage/FileStorage`로 S3에 올리고 지운다. 버킷은 비공개다. DB에는 URL이 아닌 S3 key를 저장하고, 응답할 때 `FileStorage.createUrl`로 일정 시간(`storage.s3.url-expiration`, 1시간)만 유효한 서명 URL을 만든다.
- 로컬은 `compose.yaml`의 LocalStack이 S3를 대신한다. 버킷은 `docker/localstack/init-s3.sh`가 만든다. LocalStack은 2026.x부터 인증 토큰이 필요해서 `4.14.0`으로 고정했다. 이미지를 바꾸면 토큰 없이 뜨는지 확인한다.
- 운영은 `S3_BUCKET`(필수), `AWS_REGION`(기본 `ap-northeast-2`)을 주고, 자격 증명은 AWS 기본 방식(IAM 역할이나 `AWS_ACCESS_KEY_ID` 환경변수)을 쓴다.
- DB 행을 지울 때 S3 파일은 트랜잭션이 커밋된 뒤에 지운다. 예: `PhotoFilesDeletedEvent`.
- 채팅 사진(`POST /api/families/{id}/messages/images`)은 원본만 `families/{id}/chat/`에 올리고, `chat_messages.content`에 key를 저장한다(type `IMAGE`). 응답과 실시간 메시지의 `content`는 서명 URL이다. 가족이 삭제되면 `PhotoFilesDeletedEvent`로 함께 지운다.

## 푸시 알림 (FCM)

- 푸시는 `NotificationService.send(회원 id들, PushMessage)`로 요청한다. `PushDispatcher`가 트랜잭션이 커밋된 뒤 다른 스레드(`@Async`)에서 그 회원들의 기기 토큰으로 보내고, 롤백되면 보내지 않는다.
- `PushMessage`의 `type`(`global/push/PushType`)은 API 명세서의 푸시 알림 표의 `data.type`이다. 앱이 화면을 고르는 데 필요한 값(예: `requestId`)은 `data`에 문자열로 넣는다.
- 활동 중인 모든 회원에게 보낼 때(공지 전체 푸시)는 `NotificationService.sendToAll`을 쓴다. 회원 id를 모으지 않고 발송기가 활동 회원(`USER`·`ACTIVE`)의 토큰을 한 번에 찾는다.
- 지금 보내는 푸시: SOS 발송(보낸 사람을 뺀 가족), 채팅 메시지(앱이 꺼진 가족), 용돈 요청(받는 사람)·완료·거절(요청자), 문의 답변(문의한 회원), `push: true`로 작성한 공지(전체), 일정 알림(참여자, 없으면 가족). 새로 붙이면 서비스 테스트에서 `@RecordApplicationEvents`로 `PushRequestedEvent`를 확인한다.
- 회원은 `/api/users/me/notification-settings`로 채팅·일정·용돈·공지 알림을 끌 수 있다(`notification_settings`, 행이 없으면 모두 켜짐). `PushDispatcher`가 발송 직전에 끈 회원을 뺀다. SOS와 문의 답변은 끌 수 없다.
- "앱이 꺼져 있다"는 WebSocket에 연결돼 있지 않다는 뜻이다(`PresenceService`, `SimpUserRegistry`). 이 서버의 연결만 보므로 서버를 여러 대 띄우면 바꿔야 한다.
- 장소 도착 알림(LOC-05): 가족이 `places`(반경 50~500m, 가족당 10개)를 등록하면, 위치가 바뀔 때(`LocationChangedEvent`) `PlaceAlertService`가 반경 안에 들어오면 도착, 반경+30m 밖으로 나가면 출발로 보고 본인을 뺀 가족에게 `PLACE` 푸시를 보낸다. 지금 안에 있는지는 `place_presences`에 남겨 경계에서 알림이 되풀이되지 않게 한다. 알림 설정의 `place`로 끌 수 있다.
- 일정 알림은 `ScheduleReminder`가 서울 시간 매일 20시(내일 시작하는 일정)와 8시(오늘 시작하는 일정)에 보낸다. 정해진 시각에 도는 작업은 `@Scheduled`에 `zone = "Asia/Seoul"`을 준다.
- FCM이 다시 쓸 수 없다고 알려 준 토큰(`UNREGISTERED` 등)은 발송 뒤 지운다.
- Firebase 서비스 계정 키 파일 경로를 `push.fcm.credentials-path`(운영은 `FIREBASE_CREDENTIALS_PATH`)로 주면 FCM으로 보낸다. 비어 있으면 `LoggingPushSender`가 로그만 남긴다(로컬, 테스트). 경로를 줬는데 파일을 못 읽으면 서버가 뜨지 않는다.
- 테스트에서 발송을 확인하려면 `PushSender`를 `@MockitoBean`으로 바꾸고, 비동기라 `timeout()`으로 검증한다. 예: `PushDispatcherTest`.

## 시간

- 서버 기본 시간대는 `Asia/Seoul`이다. `UrijipServerApplication.main`에서 고정한다. 테스트는 `main`을 거치지 않으므로 `build.gradle`의 `test` 태스크에서 `user.timezone`으로 같은 시간대를 준다(CI는 UTC).
- 엔티티의 시간은 `LocalDateTime`이다. 응답 날짜는 ISO-8601이고 Jackson 기본값이 이미 그렇다.
- API 명세서의 `+09:00` 오프셋은 `LocalDateTime`으로는 붙지 않는다. 응답 DTO에서 `OffsetDateTime`으로 바꿔 내보낸다. 변환은 `global/time/SeoulTime`을 쓴다(JVM 기본 시간대에 기대지 않도록 시간대를 직접 지정한다).
- 요청의 `OffsetDateTime`은 Jackson이 UTC로 맞춰 읽는다. 같은 시각이므로 `SeoulTime.toLocal`로 바꿔 저장하면 된다.

## PATCH 요청

- 보낸 필드만 바꾼다. 요청 DTO는 보통 record이고, 빠진 필드는 `null`이라 그대로 둔다.
- `null`을 보내 값을 지워야 하는 필드가 있으면 record로는 "안 보냄"과 "`null`을 보냄"을 구분할 수 없다(`Optional`도 둘 다 `Optional.empty()`가 된다). 이때는 setter가 불렸는지 기록하는 클래스로 만든다. 예: `TodoUpdateRequest`.

## 보안

- `global/config/SecurityConfig`가 경로별 규칙을 정한다. 회원가입·로그인·재발급, 관리자 로그인, Swagger, `/actuator/health`만 열려 있고 나머지는 Access 토큰이 필요하다. 인증 없이 쓰는 API를 추가하면 `PUBLIC_PATHS`에 넣는다.
- 브라우저(관리자 웹)에서 오는 요청은 `CorsConfig`가 `cors.allowed-origins`(운영은 `CORS_ALLOWED_ORIGINS`)에 있는 출처만 허용한다. 로컬은 `localhost:5173`·`3000`이다. 앱은 브라우저가 아니라 상관없다.
- 토큰 검사는 `global/security/JwtAuthenticationFilter`가 한다. 컨트롤러는 `@AuthenticationPrincipal Long userId`로 요청한 회원의 id를 받는다.
- 토큰이 유효해도 정지된 회원은 403 `USER_SUSPENDED`, 탈퇴한 회원은 401 `INVALID_TOKEN`으로 막는다. HTTP는 `JwtAuthenticationFilter`, WebSocket은 `StompAuthChannelInterceptor`(CONNECT·SUBSCRIBE·SEND)가 `UserStatusChecker`(`member/service/UserStatusService`)로 확인한다. 상태는 10초 동안 기억하고, 회원 상태를 바꾸는 코드는 `UserStatusService.evict`를 불러 바로 반영한다.
- 필터 단계의 401·403은 `GlobalExceptionHandler`를 거치지 않는다. `JwtAuthenticationEntryPoint`와 `JwtAccessDeniedHandler`가 `ApiResponse` 형식으로 응답한다.
- 인증은 JWT(Access 30분, Refresh 14일), 비밀번호는 BCrypt다. 같은 이메일로 10분 안에 5번 틀리면 10분 동안 로그인(앱·관리자)을 429 `TOO_MANY_LOGIN_ATTEMPTS`로 막는다(`LoginAttemptLimiter`, 서버 메모리). 실패 횟수는 테스트 사이에도 남으므로 로그인 실패를 내는 테스트는 `reset`으로 지운다. 앱 API는 `/api/...`, 관리자 API는 `/admin/...`(ADMIN만)이다.
- 관리자가 데이터를 바꾸는 작업(정지, 처리, 답변, 작성·수정·삭제)은 변경을 마친 뒤 같은 트랜잭션에서 `AdminLogService.record`로 `admin_logs`에 남긴다. 컨트롤러는 `@AuthenticationPrincipal Long adminId`를 서비스에 넘긴다. 새 작업 종류는 `AdminAction`에 추가한다.
- 가족에 속한 데이터는 같은 가족 멤버만 조회·수정할 수 있다. `/api/families/{familyId}/...`는 요청자가 그 가족의 멤버인지 검사한다.

## WebSocket (STOMP)

- 설정은 `global/config/WebSocketConfig`다. 앱은 `/ws`로 연결하고 `/pub/...`로 보내고 `/sub/...`를 구독한다. 하트비트는 10초다.
- `/ws` 핸드셰이크는 열려 있고(`PUBLIC_PATHS`), 인증은 CONNECT 프레임의 `Authorization: Bearer` 헤더로 `global/security/StompAuthChannelInterceptor`가 한다.
- 구독은 `/sub/family/{familyId}/{이름}`만 허용하고 가족 멤버인지 검사한다. 다른 구독 경로가 필요하면 인터셉터의 패턴을 고친다.
- 연결·구독이 거절되면 `StompErrorHandler`가 ERROR 프레임의 `message` 헤더에 에러 코드(`INVALID_TOKEN`, `FAMILY_NOT_MEMBER` 등)를 넣고 연결을 끊는다.
- `@MessageMapping` 메서드는 `Principal`을 받아 `Long.valueOf(principal.getName())`로 회원 id를 얻는다. 처리 중 예외는 `StompExceptionHandler`가 로그만 남기고 메시지를 버린다.
- 가족에게 보내는 메시지는 DB 트랜잭션이 커밋된 뒤에 보낸다. 예: `LocationChangedEvent` → `LocationBroadcaster`.
- 채팅 읽음: 멤버마다 마지막으로 읽은 메시지 id(`family_members.last_read_message_id`)를 둔다. 앱이 `/pub/chat/read`로 보내거나 회원이 메시지를 보내면 앞으로만 움직이고, 움직이면 `/sub/family/{id}/read`로 `{userId, previousLastReadMessageId, lastReadMessageId}`를 보낸다. 메시지 응답의 `unreadCount`는 보낸 사람과 메시지 뒤에 들어온 멤버를 뺀, 아직 읽지 않은 멤버 수다(`UnreadCounter`).

## 테스트

- DB가 필요한 테스트는 `@Import(TestcontainersConfiguration.class)`로 실제 MySQL을 쓴다. H2는 쓰지 않는다.
- S3를 실제로 쓰는 테스트는 `LocalStackTestConfiguration`도 함께 `@Import`한다. LocalStack은 서명 없는 요청도 허용하므로, 버킷이 비공개인지는 실제 S3에서 확인해야 한다.
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
├── photo/
└── global/          # 도메인에 속하지 않는 것
    ├── config/      # 설정 클래스
    ├── entity/      # BaseTimeEntity
    ├── exception/   # ErrorCode, BusinessException, GlobalExceptionHandler
    ├── response/    # ApiResponse
    ├── time/        # SeoulTime (LocalDateTime ↔ +09:00)
    ├── storage/     # FileStorage (S3 업로드·삭제·서명 URL)
    └── security/    # JWT 설정, 토큰 발급·검증
```

위치, SOS, 사진, 용돈 등 나머지 도메인도 만들 때 같은 구조로 패키지를 추가한다.

- 호출 방향은 `controller → service → repository`다. 컨트롤러는 리포지토리를 직접 쓰지 않는다.
- 다른 도메인의 데이터가 필요하면 그 도메인의 `service`를 호출한다. 다른 도메인의 `repository`는 쓰지 않는다.
- `global`은 도메인 패키지에 의존하지 않는다.
- 엔티티를 API 응답으로 바로 내보내지 않고 `dto`로 변환한다.

이 규칙은 `ArchitectureTest`(ArchUnit)가 검사한다. 새 도메인은 위 다섯 하위 패키지를 그대로 따른다.

## 커밋과 PR

- 커밋과 푸시는 요청받았을 때만 한다.
- 커밋은 의미 있는 변경 하나 단위로 나눈다. 테스트는 그 코드를 만든 커밋에 함께 넣고, 커밋마다 빌드가 통과해야 한다.
- 메시지는 `<type>: <한국어 설명>` 형식이다. type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore` 중 하나.
- `main`에 직접 커밋하지 않는다. 작업마다 `<type>/<짧은-영문-이름>` 브랜치를 만들고 PR로 합친다. PR 제목도 커밋 메시지와 같은 형식이다.
- `main`에 강제 푸시하지 않는다. 되돌릴 때는 `git revert`를 쓴다.

## 스킬

`.claude/skills/`의 스킬 중 일부는 외부 오픈소스를 가져와 고친 것이다. 출처와 라이선스는 `.claude/skills/NOTICE.md`에 있으며, 외부 스킬을 추가하면 여기에도 기록한다.
