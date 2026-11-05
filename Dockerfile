FROM maven:3.9-eclipse-temurin-21 AS construction
WORKDIR /application
COPY pom.xml .
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /application
COPY --from=construction /application/target/*.jar application.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=Europe/Paris", "-jar", "application.jar"]
