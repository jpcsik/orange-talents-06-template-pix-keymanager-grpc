FROM openjdk:11-jdk-slim
EXPOSE 50051
ARG JAR_FILE=build/libs/*-all.jar
ADD ${JAR_FILE} app.jar
ENV APP_NAME key-manager-grpc
ENTRYPOINT ["java", "-jar", "/app.jar"]