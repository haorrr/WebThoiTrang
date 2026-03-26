# ============================================================
# Multi-stage Dockerfile — Fashion Shop API
# Stage 1: Build with Maven
# Stage 2: Run with minimal JRE
# ============================================================

FROM eclipse-temurin:22-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ============================================================
FROM eclipse-temurin:22-jre-alpine AS runtime
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod"]
