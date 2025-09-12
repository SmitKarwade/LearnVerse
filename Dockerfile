# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew .
RUN chmod +x gradlew
COPY gradle gradle
COPY . .

RUN ./gradlew clean bootJar -x test
RUN mv build/libs/*.jar app.jar

# Run stage
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

COPY --from=build /app/app.jar app.jar

# Railway sets PORT dynamically, but expose 8080 for local dev
EXPOSE 8080

#Run
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]