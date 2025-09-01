# Multi-stage build for Vue.js application
FROM node:20-alpine AS build-stage

WORKDIR /app

# Install build tools for faster builds
RUN apk add --no-cache --virtual .build-deps \
    python3 \
    make \
    g++ \
    && rm -rf /var/cache/apk/*

# Copy package files first (for better layer caching)
COPY package*.json ./

# Install dependencies with complete cache cleanup
# Use npm install instead of npm ci to avoid package-lock.json dependency
RUN npm cache clean --force \
    && npm install --no-audit --prefer-offline \
    && npm cache clean --force

    
# Copy source code (this layer changes frequently)
COPY . .

# Set environment variable to avoid path issues
ENV NODE_ENV=production

# Set VITE environment variables for build time
# These will be passed from docker build --build-arg
ARG VITE_API_BASE_URL
ARG VITE_WS_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_BASE_URL=$VITE_WS_BASE_URL

# Build the application with optimizations
# VITE_ environment variables are now injected at build time
RUN npm run build-only \
    && rm -rf node_modules

# Production stage
FROM nginx:alpine AS production-stage

# Install runtime dependencies
RUN apk add --no-cache \
    curl \
    && rm -rf /var/cache/apk/*

# Copy built files from build stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Create a script to replace environment variables at runtime
RUN echo '#!/bin/sh' > /docker-entrypoint.sh && \
    echo 'set -e' >> /docker-entrypoint.sh && \
    echo 'if [ -n "$VITE_API_BASE_URL" ]; then' >> /docker-entrypoint.sh && \
    echo '  sed -i "s|http://localhost:8080/api|$VITE_API_BASE_URL|g" /usr/share/nginx/html/assets/*.js' >> /docker-entrypoint.sh && \
    echo 'fi' >> /docker-entrypoint.sh && \
    echo 'if [ -n "$VITE_WS_BASE_URL" ]; then' >> /docker-entrypoint.sh && \
    echo '  sed -i "s|http://localhost:8080|$VITE_WS_BASE_URL|g" /usr/share/nginx/html/assets/*.js' >> /docker-entrypoint.sh && \
    echo 'fi' >> /docker-entrypoint.sh && \
    echo 'exec nginx -g "daemon off;"' >> /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

# Expose port 80
EXPOSE 80

# Use custom entrypoint script
CMD ["/docker-entrypoint.sh"]
