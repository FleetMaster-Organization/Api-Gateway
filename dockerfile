# ETAPA 1: Compilación (Maven)
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
# Copiamos el archivo de configuración de dependencias
COPY pom.xml .
# Descargamos las dependencias
RUN mvn dependency:go-offline

# Copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests

# ETAPA 2: Imagen final para correr la App (Compatible con ARM en Oracle)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Traemos el JAR generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# El puerto
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
