# Multi-stage Dockerfile for GeckoCIRCUITS REST API
# Stage 1: Build the application using Maven
# Stage 2: Create minimal runtime image with JRE 21

# =============================================================================
# Stage 1: Build Stage
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /build

# Copy parent reactor POM first (for reactor build context)
COPY pom-reactor.xml ./

# Copy core module (dependency of REST API)
COPY src/modules/gecko-simulation-core ./src/modules/gecko-simulation-core

# Copy REST API module
COPY src/modules/gecko-rest-api ./src/modules/gecko-rest-api

# Build core module first (install to local Maven repo)
WORKDIR /build/src/modules/gecko-simulation-core
RUN mvn clean install -DskipTests

# Build REST API module
WORKDIR /build/src/modules/gecko-rest-api
RUN mvn clean package -DskipTests

# =============================================================================
# Stage 2: Runtime Stage
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="GeckoCIRCUITS Team"
LABEL description="GeckoCIRCUITS REST API - Headless circuit simulation engine"
LABEL version="1.0.0"

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user for security
RUN addgroup -g 1000 gecko && \
    adduser -D -u 1000 -G gecko gecko

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/src/modules/gecko-rest-api/target/gecko-rest-api-1.0.0.jar app.jar

# Create directories for circuits and logs
RUN mkdir -p /app/circuits /app/logs && \
    chown -R gecko:gecko /app

# Switch to non-root user
USER gecko

# Expose REST API port
EXPOSE 8080

# Health check (Spring Boot Actuator health endpoint)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# Environment variables with defaults
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SERVER_PORT=8080 \
    CIRCUITS_PATH=/app/circuits \
    LOG_LEVEL=INFO

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Run the Spring Boot application
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.awt.headless=true -jar app.jar"]
