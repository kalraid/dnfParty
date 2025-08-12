# Multi-stage build for Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS build-stage

WORKDIR /app

# Copy gradle files
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle ./
COPY settings.gradle ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew build -x test --no-daemon

# Production stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy built jar from build stage
COPY --from=build-stage /app/build/libs/*.jar app.jar

# Create shared volume directory
RUN mkdir -p /app/shared-data && \
    chown -R appuser:appgroup /app

# Switch to app user
USER appuser

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
