# Mock backend for development and testing
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create shared volume directory
RUN mkdir -p /app/shared-data && \
    chown -R appuser:appgroup /app

# Copy mock server application
COPY build/libs/df-party-mock-0.0.1-SNAPSHOT.jar app.jar

# Switch to app user
USER appuser

# Expose port 8081
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/health || exit 1

# Start the mock server
ENTRYPOINT ["java", "-jar", "app.jar"]
