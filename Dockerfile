FROM openjdk:8

WORKDIR /app

# Copy the project files
COPY . /app

# Give execution permissions to gradlew
RUN chmod +x ./gradlew

# Run the build command
RUN ./gradlew clean fatJar

# Command to start your application
CMD ["java", "-jar", "path/to/yourapp.jar"]
