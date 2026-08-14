# Build em duas etapas: a imagem final carrega só o JRE e o jar, sem Maven nem código-fonte.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Camada de dependências separada: só é refeita quando o pom muda.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuário sem privilégios: se a aplicação for comprometida, o atacante não é root.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
