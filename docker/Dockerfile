# ================================================
# ProjectTraveler - Multi-Stage Dockerfile
# ================================================
# Stage 1: Build (Gradle + JDK 17)
# Stage 2: Runtime (JRE 17 Alpine - 경량 이미지)
# ================================================

# ── Stage 1: Build ──────────────────────────────
FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /app

# 의존성 캐시 최적화: 소스보다 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# 의존성만 먼저 다운로드 (소스 변경 시 캐시 재사용)
RUN gradle dependencies --no-daemon --quiet 2>/dev/null || true

# 소스 전체 복사 후 빌드
COPY src/ src/

RUN gradle bootJar --no-daemon -x test

# ── Stage 2: Runtime ─────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 보안: non-root 사용자 생성
RUN addgroup -S traveler && adduser -S traveler -G traveler

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 업로드 디렉토리 생성 (볼륨 마운트 전 기본 생성)
RUN mkdir -p /app/uploads/videos /app/uploads/images \
    && chown -R traveler:traveler /app

USER traveler

EXPOSE 8080

# JVM 튜닝: 컨테이너 메모리 자동 인식
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]