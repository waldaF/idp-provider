FROM eclipse-temurin:25-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (remove SNAPSHOT during build)
RUN mvn versions:set -DremoveSnapshot=true && \
    mvn versions:commit && \
    mvn clean package -Pdocker -DskipTests=true

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre-alpine AS runtime

# Create non-root user for security
RUN addgroup -g 1111 -S walder && \
    adduser -u 1111 -S walder -G walder

# Set working directory
WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder /app/target/idp-provider-*.jar app.jar

# Change ownership to non-root user for all app files
RUN chown -R walder:walder /app

# Switch to non-root user
USER walder

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]