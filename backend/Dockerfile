# Build: Maven + JDK 27, unit tests included.
FROM maven:3-eclipse-temurin-27 AS build
WORKDIR /app
# Dependencies first, so they are cached while only the sources change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package && cp target/roadmap-*.jar app.jar

# Run: JRE only, as a non-root user.
FROM eclipse-temurin:27-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8045
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
