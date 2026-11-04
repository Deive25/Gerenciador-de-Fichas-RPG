# 0002. Testes de caracterização com Testcontainers

Status: aceita · Incremento 1.1 · 2026-11-03

## Contexto
O projeto não tinha testes. Para refatorar e atualizar o framework com segurança, eu precisava primeiro registrar o que a API faz hoje, inclusive os bugs.

## Decisão
Testes de caracterização das 5 rotas com MockMvc e um MySQL real em container (Testcontainers), com a mesma imagem do `docker-compose.yml` (mysql:8.4). O container é único e compartilhado entre as classes de teste, o que também permite ao Spring reaproveitar o contexto. O Docker Engine 29 exige Testcontainers 1.21.4 ou superior.

## Alternativas consideradas
- H2 em memória: rápido, mas é outro banco; diferenças de SQL e de tipos podem esconder problemas (por exemplo, a coluna `int` é palavra reservada no MySQL).
- Só mocks do repositório: não exercitam o mapeamento JPA nem o esquema, justamente o que mais muda em um upgrade.

## Consequências
- Os testes verificam o banco que se usa de verdade e serviram de rede de segurança no upgrade.
- Pior: exigem Docker rodando e são mais lentos que testes unitários.
- Os testes fixam comportamentos errados de propósito; quem corrigir um bug precisa atualizar o teste correspondente.
