# Multi-stage build for SATORN backend

FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom first for better layer caching
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tzdata

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

WORKDIR /app

# Non-root runtime user
RUN addgroup -S appuser && adduser -S -G appuser appuser

# Copy artifact from build stage
COPY --from=builder /build/target/satorn-*.jar /app/app.jar
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
