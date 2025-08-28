FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]


