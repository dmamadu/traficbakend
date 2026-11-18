# Image du backend INVODIS — construite entièrement ici (plus besoin de lancer mvn avant docker build).

# ---------- 1. Compilation ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Dépendances d'abord : cette couche reste en cache tant que pom.xml ne change pas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Les tests ont besoin d'une base : ils ne sont pas lancés dans l'image
RUN mvn -B -q -DskipTests package \
 && cp target/gestionProjet-*.jar /app/app.jar

# ---------- 2. Exécution ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Utilisateur sans privilèges
RUN useradd --system --uid 10001 invodis \
 && mkdir -p /data/uploads \
 && chown -R invodis /data
USER invodis

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
