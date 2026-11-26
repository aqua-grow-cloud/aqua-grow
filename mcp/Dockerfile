# Build stage
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Cache dependencies
RUN gradle dependencies --no-daemon

COPY src src
RUN gradle assemble --no-daemon

# Run stage
FROM amazoncorretto:21-alpine
WORKDIR /app
RUN mkdir logs

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
