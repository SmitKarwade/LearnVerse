# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# First, copy all project files into the container.
# This includes the gradlew script, but it won't have execute permissions yet.
COPY . .

# NOW, make the gradlew script executable inside the container.
RUN chmod +x ./gradlew

# Now you can run the build command without a permission error.
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
