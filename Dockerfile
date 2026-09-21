# Etapa de construção
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests


# Etapa de execução
FROM eclipse-temurin:25-jre

LABEL org.opencontainers.image.source="https://github.com/IsaqueCarlos28/SinteseGeneratorApi"

WORKDIR /app

COPY --from=build /app/target/sintese-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]