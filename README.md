# Gerenciador de Fichas RPG

Aplicação de estudo full stack para cadastrar, consultar, editar e excluir fichas de personagens de RPG. O projeto conecta uma interface Angular a uma API REST em Java e Spring Boot, com persistência em MySQL.

## Tecnologias

| Camada | Stack do repositório |
| --- | --- |
| Back-end | Java 11, Spring Boot 2.5.7, Spring Web, Spring Data JPA |
| Banco de dados | MySQL |
| Front-end | Angular 13, TypeScript 4.4, Angular Material, Bootstrap |
| Documentação de API | Swagger / Springfox |
| Build | Maven e Angular CLI |

## Funcionalidades da API

O controlador `SheetPlayerController` expõe o recurso `/sheetPlayer`:

| Método | Rota | Finalidade |
| --- | --- | --- |
| GET | `/sheetPlayer` | Listar fichas |
| GET | `/sheetPlayer/{id}` | Consultar uma ficha |
| POST | `/sheetPlayer` | Cadastrar uma ficha |
| PUT | `/sheetPlayer/{id}` | Atualizar uma ficha |
| DELETE | `/sheetPlayer/{id}` | Excluir uma ficha |

O modelo contempla dados do jogador e personagem, vida, sanidade, atributos e perícias. A API permite chamadas da interface local em `http://localhost:4200`.

## Estrutura

- `Gerenciador de fichas RPG/`: aplicação Spring Boot.
- `Gerenciador de fichas RPG/src/main/java/Atos/SpringAPI/Controllers/`: endpoints REST.
- `Gerenciador de fichas RPG/src/main/java/Atos/SpringAPI/Models/`: modelo de dados.
- `Gerenciador de fichas RPG/src/main/java/Atos/SpringAPI/Repositories/`: persistência.
- `frontend/`: aplicação Angular.

## Como rodar

### Pré-requisitos

- JDK 11 ou superior (o `pom.xml` compila para Java 11; os testes e a execução foram validados com o JDK 21).
- Docker com Docker Compose (Docker Desktop no Windows). Ele é usado para o banco local **e** pelos testes.
- Node.js compatível com Angular 13, apenas se for rodar a interface.

### Banco de dados e API

As credenciais do banco **não ficam no código**: a aplicação as lê das variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Para desenvolvimento, elas ficam em um arquivo `.env` local, que não é versionado.

1. Na raiz do repositório, crie o seu `.env` a partir do modelo e troque as senhas de exemplo:

   ```sh
   cp .env.example .env
   ```

   No PowerShell: `Copy-Item .env.example .env`.

2. Suba o MySQL 8.4 em container (lê o `.env`; usa a porta 3306, então ela precisa estar livre):

   ```sh
   docker compose up -d
   ```

3. Rode o back-end. O `.env` da raiz é carregado automaticamente; se você definir as variáveis `DB_*` no ambiente, elas têm prioridade:

   ```sh
   cd "Gerenciador de fichas RPG"
   ./mvnw spring-boot:run
   ```

   No PowerShell, use `.\mvnw.cmd spring-boot:run` (no cmd: `mvnw.cmd spring-boot:run`). A API fica em `http://localhost:8080/sheetPlayer`.

   Observações:
   - O MySQL leva alguns segundos para aceitar conexões depois do `docker compose up -d`; se o back-end falhar na primeira tentativa, rode de novo.
   - O `.env` é procurado em `../.env`, relativo à pasta de onde o back-end é iniciado. Rode sempre de dentro de `Gerenciador de fichas RPG` (ou configure a IDE com esse diretório de trabalho). Se as variáveis não forem encontradas, o erro aparece como `URL must start with 'jdbc'`.
   - `DB_USERNAME` não pode ser `root`: a imagem do MySQL usa `DB_ROOT_PASSWORD` para o root e cria o usuário da aplicação separadamente.

Para parar o banco: `docker compose down` (acrescente `-v` para apagar também os dados).

### Testes

```sh
cd "Gerenciador de fichas RPG"
./mvnw -B verify
```

**Os testes precisam do Docker em execução.** Eles sobem o próprio MySQL descartável com o Testcontainers, então não usam o `.env` nem o banco do `docker compose`. O mesmo comando roda no GitHub Actions (`.github/workflows/ci.yml`).

Os testes em `src/test` registram o comportamento atual da API, inclusive comportamentos que são bugs conhecidos (por exemplo, `GET` e `DELETE` de um id inexistente), para servir de rede de segurança nas próximas refatorações.

### Interface

A interface não foi validada neste incremento. Em outro terminal:

```sh
cd frontend
npm install
npm start
```

Use uma versão do Node.js compatível com Angular 13. Acesse a interface em `http://localhost:4200` e confira a URL da API nas chamadas do front-end.

## O que este projeto demonstra

- Integração entre front-end TypeScript e API Java.
- Operações CRUD com Spring MVC e Spring Data JPA.
- Modelagem e persistência de dados de personagens.
- Documentação de endpoints com Swagger.

## Contexto do portfólio

Projeto de estudo que registra minha prática com Java, Spring Boot e Angular. É separado do meu TCC de mesa digital em Unity: [VTT_TCC](https://github.com/Deive25/VTT_TCC).
