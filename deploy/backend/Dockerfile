FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY back_end/pom.xml ./pom.xml
COPY back_end/src ./src
RUN mvn -B -q -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar ./app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
