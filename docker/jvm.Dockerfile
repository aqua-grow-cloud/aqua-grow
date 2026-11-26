# Runtime image for the Spring Boot services. The jar is built by Gradle (CI or locally), not in Docker:
#   ./gradlew :api:bootJar
#   docker build -f docker/jvm.Dockerfile --build-arg PORT=8080 -t aqua-grow-api api/build/libs
# The build context is the service's build/libs directory, which holds exactly one jar.
FROM amazoncorretto:21-alpine

ARG PORT=8080

WORKDIR /app
RUN mkdir logs

COPY *.jar app.jar

EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
