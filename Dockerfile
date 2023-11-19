# Build stage
FROM maven:3.8-openjdk-17 as build
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17
WORKDIR /app
COPY --from=build /app/target/bookapi-0.0.1-SNAPSHOT.jar /app/bookapi.jar
ENTRYPOINT ["java","-jar","/app/bookapi.jar"]
