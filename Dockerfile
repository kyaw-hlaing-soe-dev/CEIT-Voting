# =====================================================================
# Multi-stage Dockerfile for KTU Voting Application
# Builder: Maven with Eclipse Temurin 21
# Final: Minimal Java 21 runtime
# =====================================================================

# --- Build stage -------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy pom first for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build (skip tests)
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage -----------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy generated JAR from build stage
COPY --from=build /workspace/target/*.jar /app/app.jar

# Expose app port
EXPOSE 8080

# IMPORTANT: Activate prod profile for Koyeb
ENV SPRING_PROFILES_ACTIVE=prod

# Health check for container orchestration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run Spring Boot with optimized JVM settings for cloud (limited memory)
# -XX:+UseContainerSupport: Use container-aware memory settings
# -XX:MaxRAMPercentage=75.0: Use max 75% of container memory for heap
# -Djava.security.egd: Faster startup for random number generation
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
