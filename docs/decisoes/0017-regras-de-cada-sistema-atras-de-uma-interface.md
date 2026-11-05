# 0017. Regras de cada sistema atrás de `SistemaDeFicha`

Status: aceita · Incremento 2.1e · 2026-11-05

## Contexto
O 2.1d deixou, no `FichaService` e no `TesteService`, cadeias de `instanceof` que escolhiam o que fazer conforme a ficha fosse de Ordem (o [ADR 0015](0015-uma-tabela-por-sistema.md) já previa trocá-las no 2.1e). Com um só sistema isso era aceitável. O D&D precisava entrar sem copiar essas cadeias: cada sistema novo faria os dois serviços crescerem e cada `if` esquecido viraria um bug.

## Decisão
- **Uma interface por sistema:** `SistemaDeFicha` reúne tudo o que muda de um sistema para outro: `criar` (valida e converte o pedido numa entidade nova), `copiar` (o `PUT`, que substitui tudo), `resposta` (a ficha completa), `resumo` (a linha do painel), `atualizarRecursos` (o `PATCH`) e `testar` (a rolagem).
- **Duas implementações:** `SistemaOrdem` e `SistemaDnd`, cada uma um `@Component` que usa o seu mapper, a sua validação cruzada e as suas regras puras (`RegrasOrdem`, `RegrasDnd`).
- **Um registro:** `SistemasDeFicha` recebe do Spring todos os `SistemaDeFicha` e os indexa pelo `Sistema`. Dois componentes para o mesmo sistema é erro de configuração e a aplicação nem sobe (`IllegalStateException`). Pedir um sistema sem implementação também lança `IllegalStateException`, e os dois casos têm teste (`SistemasDeFichaTest`).
- **O 409 continua no `FichaService`, antes da validação:** o `PUT` com outro `sistema` responde "Sistema não muda" sem chegar às regras de nenhum sistema.
- **O comportamento de Ordem não mudou:** a lógica foi movida para `SistemaOrdem`, não reescrita, e os testes de Ordem passaram sem mudar uma asserção.

## Alternativas consideradas
- **Manter os `instanceof`:** zero código novo agora, mas cada sistema altera os dois serviços em vários pontos, e o compilador não avisa de um ponto esquecido.
- **Padrão Visitor nas entidades:** o compilador exigiria todos os casos, mas as entidades JPA passariam a conhecer DTO, validação e sorteio, e o Visitor é bastante cerimônia para duas implementações.
- **Pôr a regra nos próprios records ou entidades:** parece natural, mas mistura persistência e API com regra de jogo, e a regra deixa de ser testável sem Spring e sem banco.

## Consequências
- **Sistema novo sem mexer nos serviços:** é um `@Component` que implementa a interface, um record de pedido e uma linha em `@JsonSubTypes` (mais a migração, a entidade e o mapper, como no [ADR 0015](0015-uma-tabela-por-sistema.md)). O `FichaService` e o `TesteService` ficaram só com HTTP, transação, log e 404/409.
- Cada sistema é testado sozinho, e o registro é testado à parte.
- **Pior:** há um cast `Ficha` → `FichaOrdem` / `FichaDnd` dentro de cada implementação. É seguro porque o registro escolhe a implementação pelo `sistema` da própria ficha e o 409 vem antes, mas continua sendo um cast que o compilador não confere.
- **Pior:** uma interface com seis métodos obriga cada sistema a implementar todos, mesmo os que seriam triviais.
