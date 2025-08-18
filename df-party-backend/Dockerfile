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
ENV GRADLE_OPTS="-Xmx2048m -Dfile.encoding=UTF-8"

# Copy gradle wrapper files (not required when using system gradle, but keep structure consistent)
COPY gradle/ gradle/

# Copy build.gradle
COPY build.gradle ./

# Copy source code
COPY src/ src/

# Build the application using system gradle (wrapper not required inside container)
RUN gradle clean build -x test --no-daemon -Dorg.gradle.workers.max=1 \
	&& ls -la build/libs/

# Production stage (Playwright with browsers preinstalled)
FROM mcr.microsoft.com/playwright/java:v1.53.0-jammy

WORKDIR /app

# Headless by default
ENV PLAYWRIGHT_HEADLESS=true

# Copy built jar from build stage
COPY --from=build-stage /app/build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
