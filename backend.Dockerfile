FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw
# Précharge les dépendances pour optimiser le cache Docker entre deux builds.
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src ./src
# Les tests sont exécutés en CI (mvn verify) ; l'image ne fait que packager l'application.
RUN ./mvnw -q -DskipTests package

# uniquement le JRE et le jar final.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
