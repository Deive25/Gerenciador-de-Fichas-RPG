# 0015. Uma tabela por sistema (herança JPA JOINED)

Status: aceita · Incremento 2.1d · 2026-11-04

## Contexto
O Arthur pediu que o app tenha fichas de Ordem Paranormal, D&D 5e e D&D 2024, e que o d100 próprio do projeto saia. Cada sistema tem atributos, perícias e recursos diferentes: Ordem tem 5 atributos de 0 a 5, 28 perícias com grau, NEX e PV/SAN/PE; D&D tem 6 atributos de 1 a 30, nível, 18 perícias com proficiência e outros recursos. O painel, o retrato e o CRUD, porém, são iguais para todos.

## Decisão
- **Tabela comum + uma tabela por sistema:** `ficha` guarda o que é comum (`id`, `jogador`, `personagem`, `genero`, `idade`) e a coluna `sistema`. Cada sistema tem a sua tabela, ligada 1:1 pelo id: `ficha_ordem` agora, `ficha_dnd` no 2.1e. No Java, é herança JPA `@Inheritance(JOINED)`: `Ficha` abstrata, `FichaOrdem` concreta, e `sistema` é a coluna discriminadora.
- **Perícias do sistema em tabela própria:** `ficha_ordem_pericia (ficha_id, pericia, grau)`, uma linha por perícia treinada (sem linha = destreinada), no mesmo estilo do [ADR 0010](0010-pericias-em-tabela-propria.md).
- **JSON polimórfico pelo campo `sistema`:** o corpo de entrada é uma `sealed interface FichaRequest` com `@JsonTypeInfo` + `@JsonSubTypes`; o Jackson lê `sistema`, cria o record certo (`FichaOrdemRequest`) e sistema desconhecido ou ausente dá 400. A resposta completa também leva `sistema`.
- **A listagem devolve um resumo comum:** `id`, `sistema`, `jogador`, `personagem`, `idade`, `retratoVersao` e `recursos` como **lista** (`codigo`, `atual`, `maximo`). O painel não precisa conhecer os detalhes de cada sistema, e a listagem continua sendo **uma consulta só** (o teste `ListagemSemNMaisUmTest` provou).
- **O sistema é escolhido na criação e não muda:** um `PUT` com outro `sistema` responde 409 ("Sistema não muda").

## Alternativas consideradas
- **Catálogo por sistema + tabelas genéricas (atributo/valor):** tabelas `atributo`, `pericia` e `recurso` indexadas por código. Sistema novo exige menos migrações, mas o banco deixa de conhecer as regras: não dá para ter `check` de 0–5 ou de NEX, e as consultas viram junções genéricas mais difíceis de ler e de otimizar.
- **JSONB por ficha ou por sistema:** poucas tabelas e fácil de mudar, mas o banco vira depósito de JSON, sem constraints, e a validação fica só no código. É o mesmo raciocínio do [ADR 0010](0010-pericias-em-tabela-propria.md) para as perícias.
- **Manter uma tabela só, com as colunas de todos os sistemas:** simples de mapear, mas cada ficha teria muitas colunas nulas (as do outro sistema) e nenhum `not null` útil.

## Consequências
- **O banco valida:** `ficha_ordem` tem `check` de atributos 0–5, NEX 5–99, recursos ≥ 0 e classe; `ficha_ordem_pericia` limita o grau a três valores; `ficha.sistema` só aceita os sistemas conhecidos. Com `ddl-auto=validate` o Hibernate confere o mapeamento ao subir, e isso continua sendo a prova de que entidade e migração andam juntas.
- Excluir a ficha apaga as linhas dos sistemas e das perícias em cascata, no banco.
- Cada sistema novo custa uma migração, uma entidade, um DTO de entrada, um de saída e um mapper (a regra de rolagem é uma classe pura à parte, como `RegrasOrdem`).
- **Pior:** mais arquivos por sistema do que nas alternativas genéricas.
- **Pior:** o `FichaService` tem hoje duas cadeias de `instanceof` (uma para a resposta completa e outra para o resumo). Com um só sistema é aceitável; o 2.1e troca as duas por um mapper por sistema, para o serviço não crescer a cada sistema novo.
