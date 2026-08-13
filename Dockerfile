# 프론트(Vue) + 백엔드(Spring Boot)를 하나의 이미지로 묶어서 배포한다.
# 로컬 개발 워크플로(./gradlew bootRun + client에서 pnpm dev)에는 영향 없음 — 이 Dockerfile은
# 배포 빌드 전용이다.

# ---- Stage 1: Vue 프론트엔드 빌드 ----
FROM node:20-alpine AS frontend-build
WORKDIR /app/client
RUN corepack enable
COPY client/package.json client/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY client/ ./
RUN pnpm build

# ---- Stage 2: Spring Boot 백엔드 빌드 (프론트 빌드 결과물을 정적 리소스로 함께 패키징) ----
FROM eclipse-temurin:21-jdk AS backend-build
WORKDIR /app
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
COPY --from=frontend-build /app/client/dist ./src/main/resources/static
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# ---- Stage 3: 실행용 최소 런타임 이미지 ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend-build /app/build/libs/app.jar app.jar
EXPOSE 8080
# 베이스 이미지의 시스템 기본 시간대가 UTC라, 지정 안 하면 LocalDateTime.now() 기반의 모든
# 등록일시(상품/주문/리뷰 등)가 UTC로 찍힌다. 로컬 개발 환경(KST)과 맞추기 위해 명시한다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
