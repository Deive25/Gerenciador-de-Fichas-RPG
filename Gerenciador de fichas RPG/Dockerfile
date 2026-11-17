# API do Gerenciador de Fichas RPG para o Render (2.6). Contexto de build: esta pasta.
#   docker build -t fichas-rpg-api .
#   docker run -m 512m --cpus 0.1 -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SEGREDO=... fichas-rpg-api
# Nenhum segredo entra na imagem: tudo vem de variaveis de ambiente (lista no application.properties).

# ---- 1) compila o jar (os testes ja rodaram no CI) ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# O mvnw precisa de LF e de permissao de execucao (um checkout no Windows pode trazer CRLF).
RUN sed -i 's/\r$//' mvnw .mvn/wrapper/maven-wrapper.properties && chmod +x mvnw
COPY src src
# O spring-boot-maven-plugin deixa o devtools fora do jar (excludeDevtools = true por padrao).
RUN ./mvnw -B package -DskipTests && cp target/fichas-rpg-*.jar app.jar

# ---- 2) so o JRE, com um usuario sem privilegios ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /app/app.jar app.jar
USER app
# Padrao para 512 MB e 0,1 CPU: 75% da RAM para o heap, GC serial (sem threads extras), so o compilador rapido (C1) e pilha
# menor por thread. A variavel JAVA_TOOL_OPTIONS do ambiente (Render) tem prioridade sobre este ENV, para ajustar sem rebuild.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
