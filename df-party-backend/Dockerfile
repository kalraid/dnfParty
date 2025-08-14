# Multi-stage build for Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS build-stage

WORKDIR /app

# Install build tools for faster builds
RUN apk add --no-cache --virtual .build-deps \
    curl \
    bash \
    && rm -rf /var/cache/apk/*

# Copy gradle files first (for better layer caching)
COPY gradle/ gradle/
COPY gradlew ./
COPY build.gradle ./

# Make gradlew executable and set permissions
RUN chmod +x ./gradlew

# Download dependencies only (separate from source build)
# This layer will be cached unless build.gradle changes
RUN ./gradlew dependencies --no-daemon --parallel

# Copy source code (this layer changes frequently)
COPY src/ src/

# Build with optimizations
RUN ./gradlew build -x test --no-daemon --parallel --build-cache \
    && ls -la build/libs/

# Production stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install runtime dependencies
RUN apk add --no-cache \
    wget \
    && rm -rf /var/cache/apk/*

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
