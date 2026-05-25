FROM eclipse-temurin:17

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew build

EXPOSE 8080

CMD ["java", "-jar", "build/libs/interviewmate-0.0.1-SNAPSHOT.jar"]