# Gerenciador de Fichas RPG

Aplicação de estudo full stack para cadastrar, consultar, editar e excluir fichas de personagens de RPG, com interface Angular e API REST em Java e Spring Boot, persistida em PostgreSQL, com o esquema versionado por migrações Flyway.

## Problema

Mestres e jogadores de RPG de mesa costumam manter as fichas de personagem em papel, que se perdem e são difíceis de atualizar. Este sistema guarda a ficha (dados do jogador e do personagem, vida e sanidade atual e máxima, 9 atributos e 23 perícias) em um banco de dados e permite editá-la a qualquer momento.

## Status da modernização

O projeto original é de dezembro de 2021 (Java 11, Spring Boot 2.5.7, Springfox, Angular 13). Ele está sendo modernizado de forma incremental, por refatoração e não por reescrita, com cada passo coberto por testes.

| Incremento | Entrega | Situação |
| --- | --- | --- |
| 1.1 | Base segura e testável: credenciais fora do código, `docker compose` com o banco local, testes de caracterização das 5 rotas, CI | Concluído (PR #1 aberto) |
| 1.2 | Upgrade para Java 21 e Spring Boot 4.1.2 em duas etapas, springdoc no lugar do Springfox, pacote `dev.spironello.fichasrpg` | Concluído nesta branch |
| 1.3 | Camada de serviço, DTOs, validação, erros padronizados (ProblemDetail), logs e correção dos comportamentos errados da API | Concluído nesta branch |
| 1.4a | Migrações Flyway (`ddl-auto=validate`) e troca do MySQL por PostgreSQL 17, sem mudar a API | Concluído nesta branch |
| 1.4b | Perícias em tabela própria (migração V2), sem mudar o JSON da API | Concluído nesta branch |
| 1.5 | Teste de perícia (rolagem com sucesso, sucesso extremo e falha) com aleatoriedade injetável | Concluído nesta branch |
| 1.6 | Ajustes mínimos no front-end e README final | Planejado |

## Tecnologias

| Camada | Stack do repositório |
| --- | --- |
| Back-end | Java 21, Spring Boot 4.1.2, Spring Web MVC, Spring Data JPA (Hibernate 7) |
| Banco de dados | PostgreSQL 17 (Docker), migrações com Flyway 12 |
| Front-end | Angular 13, TypeScript 4.4, Angular Material, Bootstrap |
| Documentação de API | springdoc-openapi (OpenAPI 3 e Swagger UI) |
| Testes | JUnit, MockMvc, Mockito, Testcontainers |
| Build e CI | Maven e Angular CLI; GitHub Actions (`./mvnw -B verify`) |

## Funcionalidades da API

O controlador `SheetPlayerController` expõe o recurso `/sheetPlayer`:

| Método | Rota | Finalidade | Respostas |
| --- | --- | --- | --- |
| GET | `/sheetPlayer` | Listar fichas | 200 |
| GET | `/sheetPlayer/{id}` | Consultar uma ficha | 200; 404 se não existe |
| POST | `/sheetPlayer` | Cadastrar uma ficha | 201 com `Location: /sheetPlayer/{id}`; 400 se inválida |
| PUT | `/sheetPlayer/{id}` | Substituir uma ficha | 200; 400 se inválida; 404 se não existe |
| DELETE | `/sheetPlayer/{id}` | Excluir uma ficha | 204; 404 se não existe |

Qualquer falha inesperada responde 500 com uma mensagem genérica; o detalhe fica só no log do servidor.

### Teste de perícia

O `TestePericiaController` expõe uma rota à parte, e o CRUD acima não mudou:

| Método | Rota | Finalidade | Respostas |
| --- | --- | --- | --- |
| POST | `/sheetPlayer/{id}/testes-de-pericia` | Rolar um d100 contra uma perícia da ficha | 200; 400 se `pericia` falta, não existe ou há chave desconhecida; 404 se a ficha não existe |

É POST porque o resultado é aleatório (não idempotente) e responde 200 porque nenhum recurso é criado: o teste **não é gravado**. O corpo leva o código do enum `Pericia` (tabela na seção Perícias). Exemplo, com `atribute1` (Acrobacia) igual a 40:

```http
POST /sheetPlayer/1/testes-de-pericia
{"pericia": "ACROBACIA"}

200 OK
{"pericia": "ACROBACIA", "nome": "Acrobacia", "valor": 40, "rolagem": 7, "resultado": "SUCESSO_EXTREMO"}
```

Regras (`RegraTestePericia`, código puro, `rolagem` de 1 a 100):

| Condição | Resultado | Com valor 40 |
| --- | --- | --- |
| `rolagem <= valor / 5` (divisão inteira) | `SUCESSO_EXTREMO` | 1 a 8 |
| `rolagem <= valor` | `SUCESSO` | 9 a 40 |
| acima do valor | `FALHA` | 41 a 100 |

Perícia sem valor na ficha conta como 0 e sempre falha. Não há crítico nem desastre. Decisões no [ADR 0011](docs/decisoes/0011-teste-de-pericia-e-aleatoriedade-injetavel.md).

**Entrada.** O JSON tem as 41 chaves de sempre (`nameJ`, `nameC`, `gender`, `age`, `lifeA`..., `str`..`pod`, `atribute1`..`atribute23`).
- `nameJ`, `nameC` e `gender` são obrigatórios e têm no máximo 255 caracteres. Os números não têm limite ([ADR 0008](docs/decisoes/0008-put-como-substituicao-completa.md)).
- O JSON é **estrito**: uma chave desconhecida (por exemplo `"STR"` em vez de `"str"`) devolve 400 citando a chave, em vez de ser ignorada.
- O `id` no corpo é aceito e **ignorado**, porque o Angular o envia sempre: no PUT vale o id da URL; no POST o id é gerado.
- O PUT é substituição completa: campos omitidos viram `null`.

**Erros** seguem o ProblemDetail (RFC 9457, `Content-Type: application/problem+json`). Exemplo de validação (`POST` sem `nameJ`):

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Há campos inválidos na ficha. Veja a propriedade errors.",
  "instance": "/sheetPlayer",
  "errors": { "nameJ": "é obrigatório" }
}
```

Decisões em [ADR 0007](docs/decisoes/0007-erros-padronizados-e-json-estrito.md). A API permite chamadas da interface local em `http://localhost:4200`.

Com a API rodando, a documentação interativa (Swagger UI) fica em `http://localhost:8080/swagger-ui.html` e a especificação OpenAPI em `http://localhost:8080/v3/api-docs`.

> Nota sobre rotas removidas: até o incremento 1.1, a dependência `spring-data-rest-hal-explorer`, adicionada junto com o Springfox, trazia o Spring Data REST de forma transitiva, que expunha uma segunda API CRUD não intencional e sem documentação em `/sheetPlayers`, além de `/profile` e `/explorer`. No incremento 1.2 a dependência foi removida e essas rotas passaram a responder 404. Veja o [ADR 0005](docs/decisoes/0005-springdoc-e-remocao-do-spring-data-rest.md).

## Arquitetura

Estado atual:

```mermaid
flowchart LR
    A["Angular (porta 4200)"] -->|HTTP/JSON| B[SheetPlayerController]
    B -->|SheetPlayerRequest / Response| C[SheetPlayerService]
    A -->|HTTP/JSON| T[TestePericiaController]
    T -->|TestePericiaRequest / Response| S[TestePericiaService]
    S --> G["RegraTestePericia (regra pura)"]
    S --> Q["Dado (interface) ← DadoAleatorio"]
    S --> R
    C --> M[SheetPlayerMapper]
    C --> R["SheetPlayerRepository (Spring Data JPA / Hibernate 7)"]
    R --> D[("PostgreSQL 17 (Docker)")]
    F["Flyway: migrações em db/migration"] -.->|cria o esquema| D
    X["ApiExceptionHandler (ProblemDetail)"] -.->|traduz exceções em erros HTTP| B
    B -.-> O["springdoc: /v3/api-docs"]
    T -.-> O
    X -.-> T
```

O controller cuida só de HTTP: recebe e devolve DTOs (`record`), nunca a entidade. O `SheetPlayerService` guarda as regras, as transações (`@Transactional`) e os logs, e lança `SheetPlayerNotFoundException` quando a ficha não existe. O `SheetPlayerMapper` converte DTO e entidade campo a campo, à mão. As 23 perícias ficam na tabela `ficha_pericia` (um `Map<Pericia, Integer>` na entidade); o mapper as converte de e para `atribute1`..`atribute23`, então o JSON não mudou. Os métodos de leitura do repository usam `@EntityGraph` para trazer ficha e perícias numa consulta só. O `ApiExceptionHandler` transforma as exceções em respostas ProblemDetail. O springdoc lê os controllers e gera a especificação OpenAPI.

O teste de perícia tem camadas próprias, para o CRUD ficar intacto: o `TestePericiaService` busca a ficha (404 com a mesma `SheetPlayerNotFoundException`), lê o valor da perícia (0 se ausente), pede a rolagem ao `Dado` e aplica a `RegraTestePericia`. A interface `Dado` isola a sorte: `DadoAleatorio` (`RandomGenerator`) em produção, uma rolagem fixa nos testes.

Modelo de dados (esquema criado pelas migrações V1 e V2):

```mermaid
erDiagram
    sheet_player ||--o{ ficha_pericia : "tem (0 a 23)"
    sheet_player {
        bigint id PK
        varchar namej "obrigatório"
        varchar namec "obrigatório"
        varchar gender "obrigatório"
        integer outros_14_campos "idade, vida, sanidade e 9 atributos"
    }
    ficha_pericia {
        bigint ficha_id PK, FK
        varchar pericia PK "constante do enum Pericia"
        integer valor
    }
```

`sheet_player` tem 18 colunas. Perícia sem valor não tem linha em `ficha_pericia`; no JSON ela aparece como `null`.

### Perícias

O JSON mantém os nomes `atribute1`..`atribute23` (os nomes públicos mudam só no 1.6). O vínculo está no enum `Pericia` e fixado em teste:

| Campo do JSON | Constante | Nome |
| --- | --- | --- |
| `atribute1` | `ACROBACIA` | Acrobacia |
| `atribute2` | `ATLETISMO` | Atletismo |
| `atribute3` | `ADESTRAR_ANIMAIS` | Adestrar animais |
| `atribute4` | `ATUACAO_LABIA` | Atuação/Lábia |
| `atribute5` | `CHARME` | Charme |
| `atribute6` | `ESQUIVA` | Esquiva |
| `atribute7` | `FURTIVIDADE` | Furtividade |
| `atribute8` | `LUTA_CORPO_A_CORPO` | Luta corpo a corpo |
| `atribute9` | `LUTA_ARMA_BRANCA` | Luta com arma branca |
| `atribute10` | `INTIMIDACAO` | Intimidação |
| `atribute11` | `INTUICAO` | Intuição |
| `atribute12` | `INVESTIGACAO` | Investigação |
| `atribute13` | `ARMAS_DE_FOGO_LEVES` | Armas de fogo leves |
| `atribute14` | `ARMAS_DE_FOGO_MEDIAS` | Armas de fogo médias |
| `atribute15` | `ARMAS_DE_FOGO_PESADAS` | Armas de fogo pesadas |
| `atribute16` | `PERCEPCAO` | Percepção |
| `atribute17` | `USAR_BIBLIOTECAS` | Usar bibliotecas |
| `atribute18` | `HISTORIA` | História |
| `atribute19` | `CURA` | Cura |
| `atribute20` | `MALANDRAGEM` | Malandragem |
| `atribute21` | `OCULTISMO` | Ocultismo |
| `atribute22` | `PSICOLOGIA` | Psicologia |
| `atribute23` | `ARREMESSAR` | Arremessar |

Estrutura de pastas:

- `Gerenciador de fichas RPG/`: aplicação Spring Boot.
- `Gerenciador de fichas RPG/src/main/java/dev/spironello/fichasrpg/controller/`: endpoints REST.
- `Gerenciador de fichas RPG/src/main/java/dev/spironello/fichasrpg/dto/`: records de entrada e saída da API.
- `Gerenciador de fichas RPG/src/main/java/dev/spironello/fichasrpg/service/`: regras, transações, logs, mapper, o teste de perícia e o `Dado`.
- `Gerenciador de fichas RPG/src/main/java/dev/spironello/fichasrpg/exception/`: exceções e tratamento de erros.
- `Gerenciador de fichas RPG/src/main/java/dev/spironello/fichasrpg/model/`: entidade JPA e o enum `Pericia`.
- `Gerenciador de fichas RPG/src/main/java/dev/spironello/fichasrpg/repository/`: persistência.
- `Gerenciador de fichas RPG/src/test/java/dev/spironello/fichasrpg/`: testes.
- `docs/decisoes/`: registros de decisões (ADRs).
- `frontend/`: aplicação Angular.

## Como rodar

### Pré-requisitos

- JDK 21 (o `pom.xml` compila para Java 21).
- Docker com Docker Compose (Docker Desktop no Windows). Ele é usado para o banco local **e** pelos testes.
- Node.js compatível com Angular 13, apenas se for rodar a interface.

### Banco de dados e API

As credenciais do banco **não ficam no código**: a aplicação as lê das variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. Para desenvolvimento, elas ficam em um arquivo `.env` local, que não é versionado.

1. Na raiz do repositório, crie o seu `.env` a partir do modelo e troque as senhas de exemplo:

   ```sh
   cp .env.example .env
   ```

   No PowerShell: `Copy-Item .env.example .env`.

2. Suba o PostgreSQL 17 em container (lê o `.env`; usa a porta 5432, então ela precisa estar livre):

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
   - O PostgreSQL leva alguns segundos para aceitar conexões depois do `docker compose up -d`; se o back-end falhar na primeira tentativa, rode de novo.
   - O `.env` é procurado em `../.env`, relativo à pasta de onde o back-end é iniciado. Rode sempre de dentro de `Gerenciador de fichas RPG` (ou configure a IDE com esse diretório de trabalho). Se as variáveis não forem encontradas, o erro aparece como `URL must start with 'jdbc'`.
   - Dados que já estavam no MySQL local de versões anteriores **não são migrados automaticamente** (projeto de estudo, sem produção). Para limpar o ambiente antigo: `docker compose down --remove-orphans` e, se quiser descartar os dados, `docker volume rm gerenciador-de-fichas-rpg_mysql_data` (o nome exato aparece em `docker volume ls`).
   - O esquema é criado pelo Flyway na primeira subida da API (`src/main/resources/db/migration`); o Hibernate só confere (`ddl-auto=validate`).
   - **Migração aplicada não se edita**: o Flyway guarda o checksum de cada arquivo em `flyway_schema_history` e recusa subir se um arquivo já aplicado mudar. Para corrigir, crie uma nova versão (V3, V4...). Se o seu banco **local** de desenvolvimento ficar com checksum divergente, `docker compose down -v` recria o volume, mas **apaga os dados locais**.

Para parar o banco: `docker compose down` (acrescente `-v` para apagar também os dados).

### Interface

A interface não foi validada neste incremento. Em outro terminal:

```sh
cd frontend
npm install
npm start
```

Use uma versão do Node.js compatível com Angular 13. Acesse a interface em `http://localhost:4200` e confira a URL da API nas chamadas do front-end.

## Testes

```sh
cd "Gerenciador de fichas RPG"
./mvnw -B verify
```

**Os testes de integração precisam do Docker em execução** (os unitários do serviço não). Eles sobem o próprio PostgreSQL 17 descartável com o Testcontainers, então não usam o `.env` nem o banco do `docker compose`. Usar o banco real (e não um banco em memória) é a decisão do [ADR 0002](docs/decisoes/0002-testes-de-caracterizacao-com-testcontainers.md). O mesmo comando roda no GitHub Actions (`.github/workflows/ci.yml`), em push na `main` e em pull requests.

São 69 testes, em quinze grupos:

| Grupo | Testes | O que cobre |
| --- | --- | --- |
| `SheetPlayerControllerTest` | 11 | As 5 rotas: listar, buscar, criar (201 + `Location`), atualizar e excluir (204), com ids existentes e inexistentes (404 ProblemDetail) |
| `SheetPlayerValidacaoTest` | 12 | 400 com `errors` por campo, limite de 255, chave desconhecida, tipo errado, JSON malformado, tratamento do `id` no corpo, id da rota não numérico (400 em português) e valor rejeitado fora do log |
| `SheetPlayerServiceTest` | 6 | Unitários do serviço com Mockito, sem Spring e sem Docker |
| `ApiExceptionHandlerTest` | 1 | Unitário: o 500 genérico não vaza o texto interno da exceção |
| `FichasRpgApplicationTests` | 1 | O contexto da aplicação sobe |
| `ContratoJsonTest` | 1 | As 41 chaves do JSON que o Angular consome |
| `EsquemaBancoTest` | 3 | Tabelas `sheet_player` (18 colunas) e `ficha_pericia` (nomes, tipos e nulabilidade) e prova de que o Flyway aplicou a V1 e a V2 |
| `MigracaoPericiasTest` | 1 | Migra um schema separado até a V1, insere fichas no formato antigo e confere, após a V2, as linhas de `ficha_pericia`, a ausência das colunas `atributeN` e os demais dados intactos |
| `PericiaTest` | 2 | Unitário: as 23 constantes, o nome e o campo legado de cada uma, e que os campos vão de `atribute1` a `atribute23` sem repetir |
| `SheetPlayerMapperPericiasTest` | 2 | Unitário: cada `atributeN` vai para a perícia certa e volta na mesma chave; `null` não gera entrada e remove a anterior |
| `ListagemSemNMaisUmTest` | 1 | Listar 3 fichas com perícias executa 1 consulta SQL (estatísticas do Hibernate), e não 1 + 3 |
| `DocumentacaoApiTest` | 2 | Swagger UI e OpenAPI respondem |
| `RegraTestePericiaTest` | 17 | Unitário parametrizado: fronteiras da regra (extremo, sucesso, falha, divisão inteira, valor 0 e acima de 100) e rolagem fora de 1..100 |
| `TestePericiaServiceTest` | 3 | Unitário com Mockito e `Dado` fixo: ficha com a perícia, ficha sem ela (valor 0) e ficha inexistente |
| `TestePericiaControllerTest` | 6 | O endpoint de ponta a ponta com `Dado` fixo (`@MockitoBean`): 200 (com valor e sem), 404, e 400 sem `pericia`, com `"VOAR"` e com chave desconhecida |

Os testes de `SheetPlayerControllerTest` nasceram no 1.1 como testes de caracterização, que registravam o comportamento de então, com bugs. No 1.3 os bugs foram corrigidos e esses testes foram reescritos de propósito para especificar o comportamento correto, antes do código. Comportamento atual da API:

- `GET`, `PUT` e `DELETE` de id inexistente respondem 404 com ProblemDetail.
- `POST` e `PUT` sem os campos obrigatórios (ou com texto acima de 255 caracteres) respondem 400 com `errors` por campo.
- `POST` responde 201 com o header `Location`; `DELETE` responde 204.
- Chave desconhecida (`"STR"`), tipo errado (`"age": "abc"`) e JSON malformado respondem 400.
- `PUT` substitui a ficha inteira, de propósito: campos omitidos viram `null`.
- O `id` enviado no corpo é ignorado.

## Decisões e trade-offs

Cada decisão tem um registro curto (contexto, decisão, alternativas e consequências) em `docs/decisoes/`:

- [0001 Credenciais em variáveis de ambiente](docs/decisoes/0001-credenciais-em-variaveis-de-ambiente.md): a senha saiu do código e vem do ambiente ou de um `.env` local.
- [0002 Testes de caracterização com Testcontainers](docs/decisoes/0002-testes-de-caracterizacao-com-testcontainers.md): testes contra um MySQL real antes de qualquer refatoração.
- [0003 Manter o MySQL até o Flyway](docs/decisoes/0003-manter-mysql-ate-o-flyway.md): a troca de banco só acontece junto com as migrações (feita no 1.4a, ADR 0009).
- [0009 Flyway e PostgreSQL](docs/decisoes/0009-flyway-e-postgresql.md): esquema versionado em SQL, `ddl-auto=validate` e troca de banco.
- [0010 Perícias em tabela própria](docs/decisoes/0010-pericias-em-tabela-propria.md): `ficha_pericia` no lugar de 23 colunas, enum `Pericia`, migração V2 e `@EntityGraph`; JSON inalterado.
- [0011 Teste de perícia e aleatoriedade injetável](docs/decisoes/0011-teste-de-pericia-e-aleatoriedade-injetavel.md): regra pura, interface `Dado`, perícia sem valor como 0 e o que ficou de fora (histórico, crítico).
- [0004 Upgrade em duas etapas](docs/decisoes/0004-upgrade-em-duas-etapas.md): Boot 2.5 → 3.5 → 4.1, com testes de contrato e de esquema escritos antes.
- [0005 springdoc e remoção do Spring Data REST](docs/decisoes/0005-springdoc-e-remocao-do-spring-data-rest.md): documentação nova e fim de uma API paralela acidental.
- [0006 Compatibilidade de nomes no Boot 4](docs/decisoes/0006-compatibilidade-de-nomes-no-boot-4.md): como preservei o JSON e o esquema do banco, e por que troquei a primeira solução.
- [0007 Erros padronizados e JSON estrito](docs/decisoes/0007-erros-padronizados-e-json-estrito.md): ProblemDetail, códigos HTTP e rejeição de chaves desconhecidas.
- [0008 PUT como substituição completa](docs/decisoes/0008-put-como-substituicao-completa.md): sem PATCH e sem regras numéricas inventadas.

## Perguntas técnicas (roteiro de entrevista)

**Por que refatorar em vez de reescrever?**
O projeto já funcionava e tinha um front-end acoplado ao formato do JSON. Reescrever perderia esse comportamento sem prova de que o novo é equivalente. Refatorando em passos pequenos, cada um com testes verdes, sempre sei o que mudou e posso parar em qualquer ponto.

**O que você fez com a senha do banco que estava exposta?**
Tirei do código e passei a lê-la de variáveis de ambiente ou de um `.env` ignorado pelo Git, com um `.env.example` de modelo. Como a senha antiga continua no histórico do Git, a trato como comprometida: o certo é trocá-la onde foi usada. Reescrever o histórico é uma decisão separada.

**Por que Testcontainers e não H2?**
Porque quero testar no banco que uso de verdade. O H2 tem diferenças de SQL e de tipos, e o próprio problema da coluna `int` (palavra reservada no SQL) é o tipo de coisa que um banco diferente esconderia. O custo é exigir Docker e ser um pouco mais lento.

**Como você garantiu que o upgrade não quebrou nada?**
Antes de mexer nas versões, escrevi testes de caracterização das 5 rotas e dois testes extras: um com as 41 chaves do JSON que o Angular consome e outro com o esquema da tabela. Depois atualizei e só aceitei as etapas com `clean verify` verde. Os dois testes extras pegaram justamente as duas mudanças silenciosas mais perigosas.

**Qual foi o problema mais difícil do upgrade?**
Os nomes mudando sozinhos. No Jackson 3, `getSTR()` passou a sair como `"STR"`, o que quebraria o Angular; resolvi com `@JsonProperty` explícito. No Hibernate 7, a tabela viraria `SheetPlayer` e os dados existentes sumiriam. Minha primeira solução foi uma estratégia de nomes própria de 63 linhas, que funcionava mas tratava o sintoma. A causa-raiz eram as aspas globais, usadas só por causa da coluna `int`; removi-as e deixei aspas apenas nessa coluna. O mesmo teste de esquema validou as duas soluções.

**O que era a API acidental do Spring Data REST?**
Uma dependência adicionada junto com o Springfox trazia o Spring Data REST de forma transitiva, que expunha uma segunda API CRUD sem validação e sem documentação em `/sheetPlayers`, além de `/profile` e `/explorer`. Removi a dependência e confirmei com a aplicação rodando que essas rotas respondem 404.

**Por que o DELETE de id inexistente passou a responder 200, e como ficou?**
Por causa do Spring Data 3: o `deleteById` passou a ignorar ids que não existem, em vez de lançar exceção (antes disso resultava em 500). No 1.2 não escondi a mudança: ajustei o teste e documentei. No 1.3 a camada de serviço verifica a existência e lança `SheetPlayerNotFoundException`, que vira 404.

**Por que o upgrade foi em duas etapas?**
Porque Boot 2.5 → 4.1 junta muitas mudanças (Spring 6, Jakarta, Hibernate, Jackson 3). Passando antes pelo 3.5.16, cada quebra tem uma causa mais fácil de identificar, e cada etapa fica com `clean verify` verde. Custa mais commits, mas reduz o risco.

**PUT ou PATCH?**
PUT, como substituição completa. O Angular já envia a ficha inteira, então um PATCH seria código sem cliente que o use. Aceito o custo: quem mandar uma ficha parcial apaga os outros campos. Se surgir esse cliente, o PATCH entra como um incremento próprio ([ADR 0008](docs/decisoes/0008-put-como-substituicao-completa.md)).

**Por que ProblemDetail?**
Porque é o padrão (RFC 9457) e o Spring já o suporta: não preciso inventar um formato de erro. O cliente recebe sempre `title`, `status` e `detail`, e na validação uma propriedade `errors` por campo. Mensagens em português e sem stack trace.

**Por que rejeitar campos desconhecidos no JSON?**
Porque o Jackson 3 já me mostrou que um nome de campo pode mudar sem aviso, e um `"STR"` ignorado em silêncio vira dado perdido. Com o modo estrito o erro aparece na hora, com o nome da chave. O custo é que é uma configuração global; a exceção é o `id`, que aceito e ignoro porque o Angular o envia.

**Por que trocar MySQL por PostgreSQL agora?**
Porque o roteiro do portfólio já previa o PostgreSQL e esta é a hora mais barata: o projeto não tem usuários nem dados em produção, e a rede de testes com Testcontainers me diz na hora se algo quebrou. Fiz a troca junto com o Flyway, mas em um incremento só dele, sem mudar a API. Os 34 testes que não tratam do esquema passaram sem mudar uma asserção; só o teste de esquema precisou dos tipos do PostgreSQL. O custo é que os dados do MySQL local não migram.

**Por que Flyway com `ddl-auto=validate`?**
Com `ddl-auto=update` o esquema depende de como o Hibernate decidir gerá-lo, não há histórico e uma mudança de versão pode alterar o banco sem aviso. Com Flyway cada mudança é um arquivo SQL versionado (`V1__...`), aplicado uma única vez e registrado em `flyway_schema_history`. O `validate` faz o Hibernate só conferir: se a entidade divergir da migração, a aplicação não sobe. O custo é escrever o SQL à mão e manter entidade e migração em sincronia.

**Por que tabela própria e não 23 colunas ou JSONB?**
Porque as 23 colunas `atribute1..23` não diziam nada, não dava para iterar sobre as perícias e cada perícia nova pedia um `ALTER TABLE`. O JSONB seria flexível, mas eu perderia tipo e integridade no banco e a consulta por perícia ficaria pior. Com a tabela `ficha_pericia` e um enum, o banco garante a chave e o valor inteiro, e perícia nova é uma constante. O custo é um join a mais e a lista de nomes valer só na aplicação (sem `check constraint`), o que está no [ADR 0010](docs/decisoes/0010-pericias-em-tabela-propria.md).

**Como garantiu que a migração não perde dados?**
Escrevi o teste antes da V2: ele migra um schema separado só até a V1, insere fichas no formato antigo (com `atribute1` e `atribute23` preenchidos, outras nulas e uma ficha sem nenhuma perícia) e migra até a última versão. Depois confere que as linhas de `ficha_pericia` são exatamente as esperadas, que as colunas antigas sumiram e que o resto da ficha está igual. A V2 copia antes de apagar as colunas e roda numa transação só, porque o PostgreSQL tem DDL transacional: se falhar no meio, nada fica pela metade. Também a rodei de verdade num banco novo pelo `docker compose`.

**Como evitou N+1?**
Ler as fichas e depois o mapa de perícias de cada uma geraria 1 consulta para a lista e mais 1 por ficha. Coloquei `@EntityGraph(attributePaths = "pericias")` no `findAll` e no `findById`, que faz o Hibernate trazer tudo num SELECT com join. Provei com as estatísticas do Hibernate num teste: com 3 fichas a listagem executa 1 consulta; sem o grafo, o mesmo teste contou 4. O custo é lembrar de repetir o grafo em qualquer método de leitura novo.

**Como você testa algo aleatório?**
Não testo a sorte, testo a regra. Separei a regra (`avaliar(valor, rolagem)`, uma função pura) da fonte de sorte (a interface `Dado`). A regra é testada com rolagens escolhidas por mim, nas fronteiras; o serviço e o endpoint recebem um `Dado` fixo. Não usei mock de `Random`: a interface pequena diz o que o teste espera. O custo é uma interface a mais, e a `DadoAleatorio` real só é exercitada de verdade em execução.

**Por que perícia sem valor conta como 0?**
Porque uma perícia sem valor na ficha é uma perícia que o personagem não treinou, e isso é uma situação normal do jogo, não um erro do cliente. Contar como 0 evita um erro novo e a regra continua uma só: com valor 0, qualquer rolagem de 1 a 100 falha. O custo é que uma perícia esquecida na ficha falha em silêncio; está no [ADR 0011](docs/decisoes/0011-teste-de-pericia-e-aleatoriedade-injetavel.md).

## Próximos passos

- **1.3** (concluído nesta branch): serviço, DTOs, validação, ProblemDetail e logs. Falta revisar e abrir o PR.
- **1.4a** (concluído nesta branch): migrações Flyway e troca do MySQL por PostgreSQL 17. Falta revisar e abrir o PR.
- **1.4b** (concluído nesta branch): perícias em tabela própria, com a migração V2. Falta revisar e abrir o PR.
- **1.5** (concluído nesta branch): teste de perícia com aleatoriedade injetável. Falta revisar e abrir o PR.
- **1.6**: ajustes mínimos no front-end e README final.

## O que este projeto demonstra

- Integração entre front-end TypeScript e API Java.
- Operações CRUD com Spring MVC e Spring Data JPA.
- Testes de caracterização contra banco real (Testcontainers) como rede de segurança e, depois, como especificação do comportamento correto.
- Camadas (controller, serviço, repository), DTOs com `record`, Bean Validation e erros padronizados (ProblemDetail).
- Upgrade seguro de framework (Spring Boot 2.5 → 4.1, Java 11 → 21) com testes de contrato e de esquema.
- Integração contínua com GitHub Actions e banco local reproduzível com Docker.
- Documentação de endpoints com OpenAPI e Swagger UI (springdoc).
- Regra de domínio testável: lógica pura separada da aleatoriedade, que entra por uma interface injetada.
- Documentação de decisões (ADRs) com alternativas e consequências.

## Contexto do portfólio

Projeto de estudo que registra minha prática com Java, Spring Boot e Angular. É separado do meu TCC de mesa digital em Unity: [VTT_TCC](https://github.com/Deive25/VTT_TCC).
