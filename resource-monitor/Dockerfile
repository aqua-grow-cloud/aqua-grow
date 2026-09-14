# Build stage
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN gradle dependencies --no-daemon

COPY src src
RUN gradle assemble --no-daemon

# Run stage
FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "app.jar"]
