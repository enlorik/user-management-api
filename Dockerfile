# 1) Build stage: produce a jar
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the pom (for dependency resolution) then the rest
COPY pom.xml .
COPY src ./src

# Build your jar (skipping tests for speed)
RUN mvn clean package -DskipTests

# 2) Run stage: copy the jar produced above
FROM eclipse-temurin:17
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the exact jar output from the build stage into 'app.jar'
COPY --from=build /app/target/user-management-api-0.0.1-SNAPSHOT.jar app.jar

# Configure healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Launch
ENTRYPOINT ["java","-jar","app.jar"]
