# 0003. Manter o MySQL até o Flyway

Status: concluída (substituída pelo [0009](0009-flyway-e-postgresql.md)) · Incremento 1.1 · 2026-11-03

## Contexto
Pretendo migrar para PostgreSQL. Fazer isso agora misturaria duas coisas: criar a rede de segurança de testes e trocar o banco.

## Decisão
Manter o MySQL até o incremento 1.4, quando a troca para PostgreSQL acontece junto com as migrações Flyway (o esquema passa a ser versionado em SQL em vez de depender de `ddl-auto=update`).

## Alternativas consideradas
- Trocar para PostgreSQL já no 1.1: se algo quebrasse, eu não saberia se a causa foi o banco ou a refatoração.
- Ficar no MySQL para sempre: simples, mas o roteiro do portfólio prevê PostgreSQL.

## Consequências
- Cada mudança de risco acontece sozinha, com testes verdes antes e depois.
- Pior: por mais alguns incrementos o projeto continua com `ddl-auto=update`, que não é adequado para produção, e o esquema só é versionado no 1.4.
- A troca de banco exigirá migrar os dados e ajustar o `EsquemaBancoTest`.
