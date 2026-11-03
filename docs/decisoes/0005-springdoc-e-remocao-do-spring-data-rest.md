# 0005. springdoc-openapi e remoção do Spring Data REST

Status: aceita · Incremento 1.2 · 2026-11-03

## Contexto
O Springfox está abandonado e é incompatível com o Spring 6. Além disso, a dependência `spring-data-rest-hal-explorer`, adicionada junto com ele, trazia o Spring Data REST de forma transitiva. Ele expunha uma segunda API CRUD, não intencional e sem documentação, em `/sheetPlayers`, além de `/profile` e `/explorer`.

## Decisão
Trocar o Springfox pelo springdoc-openapi (Swagger UI em `/swagger-ui.html`, OpenAPI em `/v3/api-docs`), coberto por `DocumentacaoApiTest`. Remover o hal-explorer e, com ele, o Spring Data REST. Conferi com a aplicação rodando que `/sheetPlayers`, `/profile` e `/explorer` respondem 404.

## Alternativas consideradas
- Manter o Springfox: não funciona com o Spring 6.
- Manter o Spring Data REST: seria uma API paralela sem validação nem documentação, que o front-end não usa.

## Consequências
- Há uma única API, e a documentação é gerada a partir do código.
- Pior: quem usasse as rotas acidentais deixa de poder usá-las.
- Pior: a versão do springdoc fica fixa no `pom.xml`, porque o Boot não a gerencia; precisa ser atualizada manualmente.
