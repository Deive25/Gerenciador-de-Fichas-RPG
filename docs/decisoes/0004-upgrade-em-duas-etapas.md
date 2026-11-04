# 0004. Upgrade em duas etapas, com testes antes

Status: aceita · Incremento 1.2 · 2026-11-03

## Contexto
O projeto estava em Java 11 e Spring Boot 2.5.7. O caminho até Boot 4.1.2 e Java 21 cruza mudanças grandes (Spring 6, Jakarta, Hibernate 6 e 7, Jackson 3).

## Decisão
Atualizar em duas etapas, cada uma com `clean verify` verde: Boot 2.5.7 → 3.5.16 e depois 3.5.16 → 4.1.2, com Java 11 → 21. Antes, escrevi dois testes: `ContratoJsonTest` (as 41 chaves do JSON que o Angular consome) e `EsquemaBancoTest` (nomes, tipos e nulabilidade das colunas de `sheet_player`). No mesmo incremento, o CI passou a rodar em push só na `main` e em pull requests, evitando execução dupla em branch com PR.

## Alternativas consideradas
- Salto direto 2.5 → 4.1: menos commits, mas mistura as causas quando algo quebra.
- Testar só pelo comportamento das rotas: não pegaria mudanças silenciosas no JSON nem no esquema.

## Consequências
- Cada quebra apareceu em um teste e teve causa identificável.
- Mudança de comportamento aceita: com o Spring Data 3, `DELETE` de id inexistente deixou de dar 500 e passou a responder 200 sem corpo. Ajustei o teste e documentei; o 404 vem no 1.3.
- Pior: duas etapas dão mais trabalho e mais commits do que o salto único.
