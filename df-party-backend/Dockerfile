# Multi-stage build for Spring Boot application
FROM eclipse-temurin:17-jdk-alpine AS build-stage

WORKDIR /app

# Install build tools for faster builds
RUN apk add --no-cache --virtual .build-deps \
	curl \
	bash \
	gradle \
	&& rm -rf /var/cache/apk/*

# Increase Gradle memory and set encoding to avoid OOM during bootJar
ENV GRADLE_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
ENV JAVA_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=512m"

# Copy gradle wrapper files (not required when using system gradle, but keep structure consistent)
COPY gradle/ gradle/

# Copy build.gradle
COPY build.gradle ./

# Copy source code
COPY src/ src/

# Build the application with optimized settings
RUN gradle clean build -x test --no-daemon \
	-Dorg.gradle.jvmargs="-Xmx4096m -XX:MaxMetaspaceSize=512m" \
	-Dorg.gradle.workers.max=2 \
	-Dorg.gradle.parallel=true \
	-Dorg.gradle.caching=true \
	&& ls -la build/libs/

# Production stage (Playwright with browsers preinstalled)
FROM mcr.microsoft.com/playwright/java:v1.53.0-jammy

WORKDIR /app

# Headless by default
ENV PLAYWRIGHT_HEADLESS=true

# Set Playwright cache and browser paths for Kubernetes
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PLAYWRIGHT_CACHE_DIR=/tmp/.playwright

# Create necessary directories with proper permissions
RUN mkdir -p /ms-playwright /tmp/.playwright && \
    chmod 755 /ms-playwright /tmp/.playwright

# Copy built jar from build stage
COPY --from=build-stage /app/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
