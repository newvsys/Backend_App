# ─────────────────────────────────────────────
# Stage 1: Build the application
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM first (layer-cache friendly)
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./

# Make the Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy the full source and build the fat JAR (skip tests – run them separately in CI)
COPY src ./src
RUN ./mvnw package -B -DskipTests

# ─────────────────────────────────────────────
# Stage 2: Runtime image
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install su-exec for privilege dropping in the entrypoint
RUN apk add --no-cache su-exec

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create the logs directory and set ownership
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Copy the fat JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Copy and enable the entrypoint script (runs as root to fix volume permissions)
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose the application port
EXPOSE 8080

# JVM tuning: use container-aware memory settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["/entrypoint.sh"]

