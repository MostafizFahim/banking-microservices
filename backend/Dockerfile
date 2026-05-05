# Use official Maven image with Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY account-service/pom.xml ./account-service/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn clean package -DskipTests

# Use official Java 21 runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/account-service/target/account-service-*.jar app.jar
EXPOSE 8081


ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]