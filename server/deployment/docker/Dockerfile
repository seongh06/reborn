FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN echo "KAKAO_APP_KEY=dummy" > local.properties
RUN echo "BASE_URL=https://dummy.local" >> local.properties
RUN ./gradlew :server:bootJar -x test --no-daemon
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S reborn && adduser -S reborn -G reborn
COPY --from=build /app/server/build/libs/*.jar app.jar
RUN chown reborn:reborn app.jar
USER reborn
ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]