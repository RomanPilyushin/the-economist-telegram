FROM openjdk:11

# Copy the fat JAR into the container
COPY build/libs/ThEconomistDownloader-1.0-SNAPSHOT.jar /app.jar

# Command to run the application
CMD ["java", "-jar", "/app.jar"]
