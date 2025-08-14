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

# Install dependencies with optimizations
RUN npm ci --cache .npm --prefer-offline --no-audit \
    && npm cache clean --force

# Copy source code (this layer changes frequently)
COPY . .

# Set environment variable to avoid path issues
ENV NODE_ENV=production

# Build the application with optimizations
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

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
