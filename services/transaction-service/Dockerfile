# ====================================================================
# Build Stage
# Use Maven with JDK 21 on Alpine Linux for a small build footprint
# ====================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 1. Copy dependency definitions first to leverage Docker layer caching
# If pom.xml hasn't changed, dependencies won't be re-downloaded
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# 2. Download dependencies (offline mode)
RUN ./mvnw dependency:go-offline

# 3. Copy source code and build the application
COPY src ./src
# Skip tests during build to speed up deployment (tests should run in CI pipeline)
RUN ./mvnw clean package -DskipTests

# ====================================================================
# Run Stage
# Use JRE 21 on Alpine for a lightweight and secure runtime
# ====================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 1. Create a non-root group and user for security
# Running as root is a security risk in containers
RUN addgroup -S triobank && adduser -S triobank -G triobank

# 2. Copy the built artifact from the builder stage
# We assume the version is 0.0.1-SNAPSHOT based on pom.xml
COPY --from=builder /app/target/transaction-service-0.0.1-SNAPSHOT.jar app.jar

# 3. Set ownership to the non-root user
RUN chown triobank:triobank app.jar

# 4. Switch to non-root user
USER triobank:triobank

# 5. Expose the application port
# Based on application.yaml configuration (server.port: 8084)
EXPOSE 8084

# 6. Define environment variables with sensible defaults
# These can be overridden at runtime (e.g., in Kubernetes values.yaml)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE="prod"
ENV SERVER_PORT="8084"

# 7. Add Healthcheck
# Docker/K8s can use this to verify if the service is healthy
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# 8. Entrypoint
# simple "java -jar" command with environment options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
