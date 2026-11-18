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
# Extrai o jar em app.jar (fino) + lib/: o CDS precisa das classes em jars comuns, nao no jar aninhado do Spring Boot.
RUN java -Djarmode=tools -jar app.jar extract --destination extracted

# ---- 2) so o JRE, com um usuario sem privilegios, e o arquivo CDS (Class Data Sharing) para arrancar mais rapido ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /app/extracted /app
# Training run: sobe o contexto Spring ate o refresh e sai (spring.context.exit=onRefresh); a JVM grava em app.jsa
# as classes carregadas, ja pre-processadas. Roda no build, SEM banco: as propriedades -D abaixo so valem neste RUN
# (Flyway e DDL desligados, sem consultar metadados do JDBC, Hikari sem conectar na partida) e NUNCA na execucao real.
# O segredo JWT abaixo e ficticio e so existe nesta linha. O .jsa so vale com este mesmo JRE e estes mesmos jars.
RUN java -XX:ArchiveClassesAtExit=/app/app.jsa -Dspring.context.exit=onRefresh \
      -Dspring.datasource.url=jdbc:postgresql://localhost:5432/treino \
      -Dspring.datasource.username=treino -Dspring.datasource.password=treino \
      -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
      -Dspring.flyway.enabled=false -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect \
      -Dapp.jwt.segredo=segredo-ficticio-so-do-treino-no-build-0123456789abcdef \
      -jar /app/app.jar
USER app
# Padrao para 512 MB e 0,1 CPU: 75% da RAM para o heap, GC serial (sem threads extras), so o compilador rapido (C1) e pilha
# menor por thread. A variavel JAVA_TOOL_OPTIONS do ambiente (Render) tem prioridade sobre este ENV, para ajustar sem rebuild.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
# Se o .jsa nao bater com a JVM/classpath, o java so avisa e segue sem CDS (nao quebra).
ENTRYPOINT ["java", "-XX:SharedArchiveFile=/app/app.jsa", "-jar", "/app/app.jar"]
