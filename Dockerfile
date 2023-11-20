# Build stage
FROM maven:3.8-openjdk-17 as build
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Run stage
FROM maven:3.8-openjdk-17
WORKDIR /app
COPY --from=build /app/target/bookapi-0.0.1-SNAPSHOT.jar /app/bookapi.jar
COPY --from=build /app /app
EXPOSE 8080
CMD ["java", "-jar", "/app/bookapi.jar"]
