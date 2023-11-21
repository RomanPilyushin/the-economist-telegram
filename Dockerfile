FROM openjdk:8
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean fatJar
CMD ["java", "-jar", "build/libs/ThEconomistDownloader-1.0-SNAPSHOT.jar"]
