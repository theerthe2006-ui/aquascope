# Multi-stage Dockerfile for AQUASCOPE Spring Boot Backend
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package application and skip tests in container build (tests are run in CI/local)
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user and directory for persistent evidence uploads
RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN mkdir -p /var/data/aquascope/uploads && chown -R spring:spring /var/data/aquascope

# Copy compiled jar from build stage
COPY --from=build /app/target/aquascope-backend-*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

ENV SPRING_PROFILES_ACTIVE=prod
ENV UPLOAD_DIR=/var/data/aquascope/uploads
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=8080", "-jar", "app.jar"]
