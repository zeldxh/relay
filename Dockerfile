# syntax=docker/dockerfile:1

# --- build ---
FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /build

# Cache dependencies in their own layer, invalidated only when pom.xml changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S relay && adduser -S relay -G relay
USER relay

COPY --from=build /build/target/relay-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
