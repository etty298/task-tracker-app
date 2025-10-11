# Используем OpenJDK 17
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/task-tracker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]