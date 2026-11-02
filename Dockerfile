# syntax=docker/dockerfile:1

# ---- build: compile with the Gradle wrapper and split the boot jar into layers ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

# Dependencies first, so code changes don't re-download them.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar \
	&& java -Djarmode=tools -jar build/libs/home-portal-0.0.1-SNAPSHOT.jar extract --layers --launcher --destination /layers

# ---- run: JRE only, non-root ----
FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home homeportal
WORKDIR /app

# Least- to most-often changed, so rebuilds reuse the dependency layers.
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./

USER homeportal
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
