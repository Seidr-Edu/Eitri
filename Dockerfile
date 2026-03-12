FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre

RUN useradd -u 10001 -r -g root -d /app -s /usr/sbin/nologin appuser && \
    mkdir -p /app /run && \
    chown -R appuser:root /app /run

WORKDIR /app
COPY --chown=appuser:root --from=build /app/target/*-jar-with-dependencies.jar /app/eitri.jar
COPY --chown=appuser:root eitri-service.sh /app/eitri-service.sh

RUN chmod +x /app/eitri-service.sh

USER appuser

ENTRYPOINT ["/app/eitri-service.sh"]
