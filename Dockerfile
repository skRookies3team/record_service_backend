FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY build/libs/*.jar record-service.jar
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-jar","record-service.jar"]