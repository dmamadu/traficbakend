# Use the Eclipse Temurin OpenJDK 17 image from Docker Hub (openjdk:17 was deprecated/removed)
FROM eclipse-temurin:17-jre
# Set working directory inside the container
WORKDIR /app
# Copy the compiled Java application JAR file into the container
COPY ./target/gestionProjet-0.0.1-SNAPSHOT.jar /app
# Expose the port the Spring Boot application will run on
EXPOSE 8081
# Command to run the application
CMD ["java", "-jar", "gestionProjet-0.0.1-SNAPSHOT.jar"]