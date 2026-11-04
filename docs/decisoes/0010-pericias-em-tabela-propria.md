# 0010. Perícias em tabela própria

Status: aceita · Incremento 1.4b · 2026-11-03

## Contexto
As 23 perícias eram colunas `atribute1..atribute23` de `sheet_player`: nomes sem significado, nenhuma forma de iterar sobre elas e uma coluna nova a cada perícia nova. O JSON consumido pelo Angular usa esses mesmos nomes e não pode mudar neste incremento (os nomes públicos ficam para o 1.6).

## Decisão
- Tabela `ficha_pericia (ficha_id, pericia, valor)`, chave primária `(ficha_id, pericia)`, `on delete cascade`. Perícia sem valor não gera linha; no JSON ela continua saindo como `null`.
- Enum `Pericia` com nome de exibição e `campoLegado` explícito (`"atribute1"`...), não `ordinal()`; o mapeamento está fixado em teste. No banco grava-se o nome da constante (`ACROBACIA`).
- Entidade com `Map<Pericia, Integer>` (`@ElementCollection`); o mapper converte de e para `atributeN`. PUT segue como substituição completa: perícia `null` remove a linha.
- Migração `V2__move_pericias_para_tabela_propria.sql` cria a tabela, copia os dados e remove as colunas. O PostgreSQL tem DDL transacional, então a V2 é atômica. Testada com dados do formato antigo (`MigracaoPericiasTest`).
- `@EntityGraph` em `findAll` e `findById` traz as perícias no mesmo SELECT (teste: 1 consulta para 3 fichas; sem o grafo eram 4).
- Sem `check constraint` com a lista de perícias: o enum é a fonte da verdade.

## Alternativas consideradas
- Manter as 23 colunas: zero migração, mas sem nomes claros, sem iteração e com ALTER TABLE a cada perícia nova.
- JSONB: flexível, mas perde tipo, integridade e consulta simples por perícia; o esquema ficaria só na aplicação.
- Tabela catálogo de perícias (com FK): permite perícias novas sem deploy, porém exige gerir um cadastro que ninguém pediu; o enum basta enquanto a lista é fixa.

## Consequências
- Perícia nova vira uma constante no enum, sem migração; o esquema e o JSON de hoje continuam iguais.
- Pior: sem constraint no banco, um valor inválido gravado por fora (SQL manual) só falha ao ler a ficha; renomear uma constante exige uma migração que atualize as linhas.
- Pior: cada leitura faz um join a mais, e esquecer o `@EntityGraph` num método novo reintroduz o N+1.
- Pior: o mapper ainda liga `atributeN` às perícias à mão, duplicando o que o enum já diz; só some quando o JSON mudar (1.6).
