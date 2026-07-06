# Fase 1: Compilación
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Fase 2: Ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app
# CAMBIAMOS demo-0.0.1 POR mass-0.0.1 AQUÍ:
COPY --from=build /app/target/mass-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]