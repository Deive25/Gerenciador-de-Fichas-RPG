# 0016. Saída do sistema d100 e conversão das fichas para Ordem Paranormal

Status: aceita · Incremento 2.1d · 2026-11-04 · Decisão do Arthur

## Contexto
O sistema d100 do projeto (9 atributos, 23 perícias, a regra de sucesso extremo do [ADR 0011](0011-teste-de-pericia-e-aleatoriedade-injetavel.md)) era próprio e vinha desde 2021. Com os sistemas reais (Ordem Paranormal agora, D&D no 2.1e), ele não tem mais lugar no app. Mas já havia fichas no banco: a do Arthur e as de demonstração, algumas com retrato. O que fazer com elas?

## Decisão
- **Nenhuma ficha é apagada.** A migração V4 renomeia `sheet_player` para `ficha` (e `namej`/`namec`/`gender`/`age` para `jogador`/`personagem`/`genero`/`idade`). Os **ids não mudam**, então o retrato (`retrato_ficha`, que aponta para o id) e as URLs `/fichas/{id}` continuam valendo.
- **Cada ficha antiga vira uma ficha de Ordem:** NEX 5 e os 5 atributos em 1 (valores válidos e neutros; 0 mudaria a regra da rolagem), PV e SAN copiados de vida e sanidade, PE vazio, classe e origem vazias e **nenhuma perícia** (as escalas d100 e d20 não se convertem). Só depois da cópia a V4 apaga `ficha_pericia` e as colunas do d100.
- **A rota muda:** `POST /fichas/{id}/testes-de-pericia` sai e entra `POST /fichas/{id}/testes`, que rola perícia ou atributo. Não há rota antiga mantida, como no [ADR 0012](0012-contrato-com-nomes-claros.md): o único cliente é o Angular, que mudou no mesmo incremento.
- **O ADR 0011 deixa de valer para a regra** (d100, sucesso extremo, perícia sem valor como 0). Continua valendo a parte de a sorte entrar por uma interface injetável (`Dado`), que o teste de Ordem reaproveita.

## Alternativas consideradas
- **Manter o d100 como um quarto sistema:** preservaria tudo, mas o app ficaria com um sistema que ninguém mais joga, mais um formulário, uma tabela e uma regra para manter. O Arthur preferiu que saísse.
- **Converter as fichas para D&D:** D&D só chega no 2.1e, e os 9 atributos d100 também não casam com os 6 de D&D.
- **Apagar as fichas antigas:** o jeito mais simples, mas o Arthur perderia a própria ficha e o retrato. A conversão custa uma migração e um teste.

## Consequências
- Nada some além do previsto. O `MigracaoSistemasTest` migra um schema separado até a V3, insere três fichas d100 (uma com vida, sanidade, perícias e retrato; outra sem vida nem sanidade; outra sem perícias), aplica a V4 e confere nome, jogador, gênero, idade, PV e SAN, `sistema`, NEX 5, atributos 1, nenhuma perícia, o retrato intacto e a ausência das colunas e da tabela antigas.
- Antes de aplicar a V4 no banco do Arthur foi feito um **backup local**.
- **Pior:** os atributos e as perícias do d100 se perdem **de propósito**. Se alguém quiser os números antigos, só no backup.
- **Pior / risco:** a V4 copia vida e sanidade para colunas com `check (>= 0)`. Um banco d100 com vida ou sanidade **negativas** faria a V4 falhar, e a API não subiria. Como o PostgreSQL tem DDL transacional, o Flyway desfaz a transação inteira e nenhum dado se perde; a correção seria ajustar o dado antes e subir de novo.
- O arquivo V4 não será alterado depois de aplicado: mudar o conteúdo muda o checksum, e o Flyway recusa subir. Correções entram numa V5.
