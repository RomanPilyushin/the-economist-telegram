# Use OpenJDK 17 as the base image
FROM openjdk:17

# Set the working directory inside the container
WORKDIR /app

# Install necessary tools, including xargs
RUN apk --no-cache add findutils

# Copy the local code to the container
COPY . .

# Set the appropriate file permissions for Gradle
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew clean fatJar

# Set the time zone
ENV TZ=Europe/Kiev

# Specify the command to run the application
CMD ["java", "-jar", "build/libs/ThEconomistDownloader-1.0-SNAPSHOT.jar"]
