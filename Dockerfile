FROM gradle:9.2.1-jdk21 AS builder
WORKDIR /opt/app
COPY build.gradle settings.gradle ./
RUN gradle build -x test --parallel --build-cache || true
COPY src ./src
RUN gradle build -x test --parallel --build-cache

FROM eclipse-temurin:21-jre
WORKDIR /opt/app
COPY --from=builder /opt/app/build/libs/*.jar /opt/app/app.jar
ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]