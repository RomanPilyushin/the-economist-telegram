# Use multi-stage builds to separate the build environment from the runtime environment
# Build stage
FROM openjdk:8 as builder
WORKDIR /build
COPY . .
RUN chmod +x ./gradlew && ./gradlew clean fatJar

# Runtime stage
FROM openjdk:8-jre-slim
WORKDIR /app

# Copy only the built JAR file from the build stage
COPY --from=builder /build/build/libs/ThEconomistDownloader-1.0-SNAPSHOT.jar .

# Set the command to run your app (adjust as necessary)
CMD ["java", "-jar", "ThEconomistDownloader-1.0-SNAPSHOT.jar"]

# (Optional) Expose the port if your app is a web service
EXPOSE 8080
