# 1단계: 빌드 환경 (JDK 17)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 컨텍스트는 프로젝트 루트이므로 전체 파일 복사 (.dockerignore로 필터링)
COPY . .

# 실행 권한 부여 및 서버 모듈 빌드 (테스트 제외)
RUN chmod +x gradlew
RUN ./gradlew :server:bootJar -x test

# 2단계: 실행 환경 (JRE 17)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 단계에서 생성된 jar 파일 복사
COPY --from=build /app/server/build/libs/*.jar app.jar

# 환경변수 기본값 설정
ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=prod

# 8080 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
