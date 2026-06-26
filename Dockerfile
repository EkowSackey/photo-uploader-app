FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /app/target/fotos-0.0.1-SNAPSHOT.jar app.jar

RUN chown app:app app.jar && mkdir -p /app/uploads && chown app:app /app/uploads

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
