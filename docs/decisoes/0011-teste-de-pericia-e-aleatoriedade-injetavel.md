# 0011. Teste de perícia e aleatoriedade injetável

Status: aceita · Incremento 1.5 · 2026-11-03

## Contexto
A primeira regra de domínio do projeto: o jogador escolhe uma perícia da ficha e o sistema rola um d100 e diz se foi sucesso extremo, sucesso ou falha. O resultado depende de sorte, e uma regra que depende de sorte não tem teste confiável.

## Decisão
- `POST /sheetPlayer/{id}/testes-de-pericia`, corpo `{"pericia": "ACROBACIA"}`, resposta 200 com `pericia`, `nome`, `valor`, `rolagem` e `resultado`. É POST porque o resultado é aleatório (não idempotente) e é 200 porque nenhum recurso é criado. Nada é gravado.
- Regra pura em `RegraTestePericia.avaliar(valor, rolagem)`: `rolagem <= valor / 5` (divisão inteira) é `SUCESSO_EXTREMO`; senão `rolagem <= valor` é `SUCESSO`; senão `FALHA`. Rolagem fora de 1..100 lança `IllegalArgumentException`.
- A sorte fica atrás da interface `Dado` (`rolarD100()`). Em produção, `DadoAleatorio` usa `RandomGenerator`; nos testes entra uma rolagem fixa (lambda ou `@MockitoBean`).
- Perícia ausente da ficha vale 0 ("não treinada") e sempre falha. Não existe erro novo para isso.
- Erros pelo `ApiExceptionHandler` existente: `pericia` ausente, código inexistente e chave desconhecida são 400; ficha inexistente é 404.

## Alternativas consideradas
- `Random` estático ou `Math.random()` dentro do serviço: simples, mas não dá para fixar a rolagem; só testaria "o resultado é um dos três".
- `Clock` ou seed fixa: a seed torna a sequência previsível em produção, e acoplar o teste à sequência de um gerador é frágil; uma rolagem explícita diz o que o teste espera.
- Persistir o histórico de testes: ninguém pediu consultar rolagens antigas; entra só quando houver essa necessidade (YAGNI).
- Crítico e desastre (1 e 100 especiais): não estão no plano e mudariam a regra de forma que ninguém definiu; podem entrar depois como incremento próprio.

## Consequências
- A regra tem testes determinísticos em todas as fronteiras, sem Docker e sem sorte; só o serviço e o endpoint tocam em `Dado`.
- Pior: perícia digitada errada na ficha ou nunca preenchida falha em silêncio como valor 0, em vez de avisar o jogador.
- Pior: sem histórico, uma rolagem não pode ser auditada nem refeita depois.
- Pior: a regra de um quinto e a falta de crítico são simplificações; se o sistema de RPG real pedir outra, a regra muda.
