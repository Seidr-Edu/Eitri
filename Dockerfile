FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /app/target/*-jar-with-dependencies.jar /app/eitri.jar

ENTRYPOINT ["java", "-jar", "/app/eitri.jar"]
