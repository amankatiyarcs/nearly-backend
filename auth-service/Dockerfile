FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

COPY ../pom.xml ../pom.xml
COPY pom.xml .
COPY src src

RUN apk add --no-cache maven && \
    mvn package -DskipTests -f pom.xml

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN mkdir -p logs

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

