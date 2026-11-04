# 0006. Compatibilidade de nomes (JSON e banco) no Boot 4

Status: aceita · Incremento 1.2 · 2026-11-03

## Contexto
Dois nomes mudaram sozinhos no Boot 4, e os testes de contrato e de esquema pegaram os dois.
- Jackson 3: `getSTR()` passou a sair como `"STR"` em vez de `"str"` nos 9 atributos, quebrando o contrato com o Angular.
- Hibernate 7: a estratégia de nomes padrão (snake_case) não converte identificadores entre aspas. O projeto usava `globally_quoted_identifiers=true` só porque a coluna `int` é palavra reservada do MySQL. Sem correção, a tabela viraria `SheetPlayer` em vez de `sheet_player` e os dados existentes sumiriam para a aplicação.

## Decisão
- JSON: `@JsonProperty` explícito nos 9 atributos, preservando o contrato.
- Banco: sem aspas globais; aspas somente na coluna `int`.

A primeira solução para o banco foi uma `PhysicalNamingStrategy` própria de 63 linhas. Funcionava, mas tratava o sintoma. Troquei pela causa-raiz: o uso desnecessário de aspas globais. O `EsquemaBancoTest` validou as duas soluções sem ser alterado.

## Alternativas consideradas
- Manter a estratégia de nomes própria: mais código para manter, escondendo o motivo real.
- Mudar a configuração global do Jackson: seria um ajuste amplo para um problema de 9 campos.

## Consequências
- Menos código e contrato com o Angular preservado, com testes que provam isso.
- Pior: os nomes dos métodos continuam estranhos (`getSTR`); a limpeza pode vir com os DTOs no 1.3.
- Pior: atributos enviados com nome em maiúsculas (`"STR"`) são ignorados sem aviso; o comportamento está registrado nos testes.
