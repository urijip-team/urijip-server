# 빌드: Gradle로 실행 jar를 만들고 의존성과 앱 코드를 층으로 나눈다. 테스트는 CI에서 돌린다.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# 의존성 다운로드를 소스와 나눠, 소스만 바뀌면 이 층을 다시 쓰게 한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar \
	&& java -Djarmode=tools -jar build/libs/urijip-server-*-SNAPSHOT.jar extract --layers --launcher --destination extracted

# 실행: JRE만 담고 root가 아닌 사용자로 띄운다.
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
	JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
