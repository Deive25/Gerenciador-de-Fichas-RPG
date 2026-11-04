# 0007. Erros padronizados (ProblemDetail) e JSON de entrada estrito

Status: aceita · Incremento 1.3 · 2026-11-03

## Contexto
A API devolvia 200 com o texto `null` para ficha inexistente, 200 no DELETE, 500 para campos obrigatórios ausentes e ignorava em silêncio chaves desconhecidas como `"STR"` (um erro de digitação do cliente virava dado perdido). Cada erro tinha um formato diferente, ou nenhum.

## Decisão
- Erros no formato ProblemDetail (RFC 9457, `application/problem+json`), tratados em um `@RestControllerAdvice` que estende `ResponseEntityExceptionHandler`. Mensagens em português.
- Códigos: GET/PUT/DELETE de id inexistente → 404; validação, tipo errado, JSON malformado ou chave desconhecida → 400; POST → 201 com `Location`; DELETE → 204; qualquer outra falha → 500 genérico (stack trace só no log).
- Validação devolve a propriedade extra `errors` (campo → mensagem).
- JSON de entrada estrito (`fail-on-unknown-properties=true`). O `id` no corpo é aceito e ignorado, porque o Angular o envia sempre: vale o id da URL (PUT) ou o gerado (POST).

## Alternativas consideradas
- Formato próprio de erro (`{codigo, mensagem}`): inventa um padrão que o ProblemDetail já define e o Spring já suporta.
- Continuar ignorando chaves desconhecidas: compatível, mas esconde erros do cliente.
- Rejeitar também o `id` no corpo: seria mais puro, mas quebraria o Angular atual.

## Consequências
- Um formato único de erro, documentado e testado; o cliente distingue 400 de 404 de 500.
- Pior: o modo estrito é global (afeta todo JSON de entrada) e um campo novo no Angular sem o back-end correspondente passa a dar 400.
- Pior: ignorar o `id` do corpo é uma exceção à regra "estrito", mantida só por compatibilidade.
