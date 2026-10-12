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

## Execução local

Este é um projeto de estudo com dependências de versões anteriores. Os comandos abaixo descrevem a estrutura existente; a execução não foi validada nesta revisão de documentação.

### API

1. Instale o JDK 11 e disponibilize um banco MySQL local.
2. Configure URL, usuário e senha do banco em seu ambiente local. A aplicação lê a configuração de `src/main/resources/application.properties`; não publique credenciais.
3. Na pasta do back-end, execute:

```sh
cd "Gerenciador de fichas RPG"
./mvnw spring-boot:run
```

No Windows, utilize `mvnw.cmd spring-boot:run`.

### Interface

Em outro terminal:

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
