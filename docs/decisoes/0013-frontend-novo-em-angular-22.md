# 0013. Frontend novo em Angular 22, em vez de atualizar o Angular 13

Status: aceita · Incremento 2.1 · 2026-11-04

## Contexto
O frontend era Angular 13 (fora de suporte), compilava só em Node ≤ 16 e não tinha nenhum teste automatizado. A fase 2 (mesa ao vivo) pede um visual novo, pensado primeiro para o celular, e telas que vão crescer (login, campanhas, rolagens ao vivo). O frontend antigo tinha só 3 formulários e um painel, e o design ia mudar inteiro de qualquer jeito.

## Decisão
- Recriar o frontend em `frontend/`, na versão estável atual (Angular 22.2), com componentes standalone, *signals*, sintaxe `@if/@for`, sem zone.js e com Vitest, contra a mesma API (contrato do 1.6, sem mudança no backend).
- Usar Angular Material 22 como base acessível (campos, abas, dicas), com o tema "investigação sombria" por variáveis CSS e fontes só do sistema.
- Fazer testes primeiro e conferir no navegador, nas larguras de celular (390 px) e de computador (1280 px).
- Reaproveitar da versão antiga só os textos de ajuda de atributos e perícias.

## Alternativas consideradas
- **Atualizar o Angular 13 versão a versão até o 22** (`ng update` 9 vezes): repete a história de "upgrade seguro" que o backend já conta e gasta esforço em telas que seriam refeitas logo depois.
- **Trocar para React/Next.js:** forte no mercado internacional, mas tira o projeto do combo Java + Angular, comum nas vagas Java no Brasil.

## Consequências
- O frontend fica na versão atual, com 43 testes unitários no CI (Node 24), e o container `node:16` deixa de ser necessário.
- Telas novas (login, campanhas) entram num projeto moderno e testável.
- **Pior:** é uma reescrita do frontend, não uma refatoração. O comportamento das telas antigas não foi preservado por testes, só a API.
- **Pior:** o Angular Material pesa no pacote. Detalhe e formulário carregam sob demanda para o pacote inicial ficar em ~300 kB (~81 kB transferidos).
