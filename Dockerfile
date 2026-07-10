# syntax=docker/dockerfile:1

# ---- Etapa de build -------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copiar primero el wrapper y el pom para aprovechar la cache de capas de Docker
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Copiar el código fuente y empaquetar
COPY src src
RUN ./mvnw -B clean package -DskipTests

# ---- Etapa de runtime -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S techcup && adduser -S techcup -G techcup
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
RUN chown techcup:techcup app.jar
USER techcup

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
