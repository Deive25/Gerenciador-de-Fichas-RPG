# 0008. PUT como substituição completa, sem regras numéricas

Status: aceita · Incremento 1.3 · 2026-11-03

## Contexto
O `PUT /sheetPlayer/{id}` sempre copiou todos os campos do corpo: o que não vem no JSON vira `null`. Isso parecia um bug nos testes de caracterização. O Angular, porém, sempre envia a ficha inteira (as 41 chaves). Também era preciso decidir quais regras de validação aplicar.

## Decisão
- O PUT é, de propósito, uma substituição completa (semântica padrão do verbo). Não há PATCH.
- Validam-se só `nameJ`, `nameC` e `gender`: obrigatórios e com até 255 caracteres, iguais às colunas `not null varchar(255)`. Os números não têm `@Min`/`@Max`.
- O teste `atualizarFichaApagaCamposOmitidos` continua valendo, agora como especificação.

## Alternativas consideradas
- PATCH (ou PUT parcial): resolveria "atualizar só a vida", mas nenhum cliente precisa disso hoje (YAGNI) e exige tratar "ausente" versus "null".
- Limites numéricos (ex.: atributo de 0 a 100): dependem do sistema de RPG e das regras do mestre, que ainda não modelei; um limite errado rejeitaria fichas legítimas.

## Consequências
- Contrato simples e previsível, igual ao que o front-end já faz.
- Pior: um cliente que mande uma ficha parcial apaga os demais campos, sem aviso.
- Pior: valores absurdos (idade negativa, atributo 9999) são aceitos; as regras de domínio podem entrar quando as perícias forem remodeladas (1.4/1.5).
