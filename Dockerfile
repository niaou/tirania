# Stage 1: build JAR
FROM eclipse-temurin:24-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# Stage 2: runtime
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
