# ======================
# Build Stage
# ======================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ======================
# Run Stage
# ======================
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Ensure logs directory exists for Log4j2 file appender
RUN mkdir -p /app/logs

# Expose ports (application + debug)
EXPOSE 8080
EXPOSE 5005

# Enable Remote Debugging
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]