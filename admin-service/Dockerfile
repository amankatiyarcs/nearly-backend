FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the jar file
COPY target/admin-service-*.jar app.jar

# Set ownership
RUN chown -R spring:spring /app

USER spring

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]

