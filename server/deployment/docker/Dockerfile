FROM amazoncorretto:21 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew :server:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine AS stage-1
WORKDIR /app
COPY --from=build /app/server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]