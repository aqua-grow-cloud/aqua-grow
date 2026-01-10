# Build stage
FROM gradle:8-jdk21-alpine AS build
WORKDIR /app

# Copy the gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (this will be cached)
RUN gradle dependencies --no-daemon

# Copy source code and build the application
COPY src src
RUN gradle assemble --no-daemon

# Run stage
FROM amazoncorretto:21-alpine
WORKDIR /app
RUN mkdir logs

# Copy the jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
