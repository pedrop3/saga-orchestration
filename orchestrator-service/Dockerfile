FROM openjdk:21
COPY build/libs/*.jar app.jar
EXPOSE 8050
ENTRYPOINT ["java", "-jar", "app.jar"]