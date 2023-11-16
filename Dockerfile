FROM openjdk:8
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean fatJar
CMD ["java", "-jar", "path/to/yourapp.jar"]
