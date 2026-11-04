# 0009. Flyway e PostgreSQL

Status: aceita · Incremento 1.4a · 2026-11-03 · conclui o [0003](0003-manter-mysql-ate-o-flyway.md)

## Contexto
O esquema nascia do `ddl-auto=update` do Hibernate: sem histórico, dependente da versão do Hibernate e inadequado para produção. O ADR 0003 adiou a troca de banco até haver migrações. Os 34 testes que não tratam do esquema já protegem o contrato, então é seguro mudar a base agora, sem tocar na API.

## Decisão
- Esquema criado só pelo Flyway: `V1__cria_tabela_sheet_player.sql` (mesmas 41 colunas e nulabilidade de antes) e `ddl-auto=validate`, que faz o Hibernate só conferir a entidade contra o banco.
- Banco PostgreSQL 17 (`postgres:17`), a mesma imagem no `docker-compose.yml` e no Testcontainers.
- Flyway pelo starter do Boot 4 (`spring-boot-starter-flyway`, Flyway 12.4.0) mais `flyway-database-postgresql`, exigido desde o Flyway 10. Driver `org.postgresql:postgresql` 42.7.14.
- Os dados do MySQL local **não são migrados** (projeto de estudo, sem produção). Para limpar: `docker compose down --remove-orphans` e, se quiser, remover o volume antigo (`gerenciador-de-fichas-rpg_mysql_data`; confira o nome em `docker volume ls`).
- A única asserção alterada foi a de tipos do `EsquemaBancoTest` (`integer`, `character varying`, `bigint`); foi acrescentada a prova do `flyway_schema_history`.

## Alternativas consideradas
- Liquibase: também maduro, mas usa changelogs em XML/YAML; SQL puro do Flyway é mais simples de ler e explicar.
- Manter `ddl-auto=update`: zero esforço, mas sem histórico nem controle do que muda no banco.
- Manter o MySQL: funciona com Flyway, mas o roteiro do portfólio prevê PostgreSQL e agora a troca é barata.

## Consequências
- O esquema é versionado, repetível (o mesmo SQL roda nos testes e no compose) e a divergência entre entidade e banco falha na hora.
- Pior: cada mudança de coluna exige escrever uma migração à mão; uma migração aplicada não pode ser editada (o Flyway valida o checksum), só corrigida por uma nova versão.
- Pior: quem tinha dados no MySQL local os perde, a menos que os exporte por conta própria.
