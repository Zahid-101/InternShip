# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory
WORKDIR /app

# Copy the pom.xml and install dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and .env file if necessary
COPY src ./src
# Using wildcard to copy .env if it exists, as spring-dotenv is used
COPY .env* ./

# Build the application, skipping tests to speed up the build
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

# Create a non-root user to run the app (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar
# Copy the .env file to the runtime stage if it exists
COPY --from=builder /app/.env* ./

# Expose the port the app runs on (App Runner defaults to 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
