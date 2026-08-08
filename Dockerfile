# Spring Boot 애플리케이션 빌드 단계
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY . .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# 실제 실행 단계
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Docker Compose의 서버 상태 확인에 사용
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --create-home --shell /usr/sbin/nologin app

COPY --chown=app:app --from=builder /workspace/build/libs/*.jar app.jar

# CI deploys this immutable runtime bundle before starting the application. Keeping the
# Compose definition and deployment scripts in the same image prevents the server from
# continuing to use stale configuration after a repository change.
COPY --from=builder /workspace/deployment/runtime /opt/bookshelf-runtime

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
