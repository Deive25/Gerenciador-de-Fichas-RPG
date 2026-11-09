# 0018. Listas da ficha em recursos próprios

Status: aceita · Incremento 2.1g · 2026-11-09

## Contexto
A ficha passa a ter listas e textos longos que o jogador edita aos poucos: a descrição e as habilidades agora, e depois o inventário, os ataques, as magias e os rituais. O `PUT /fichas/{id}` faz substituição completa ([ADR 0008](0008-put-como-substituicao-completa.md)), e o JSON é estrito ([ADR 0007](0007-erros-padronizados-e-json-estrito.md)). Pôr tudo isso no mesmo `PUT` faria o formulário da ficha crescer sem parar e cada edição pequena reenviaria a ficha inteira.

## Decisão
- **Rotas próprias:** a descrição e cada lista têm as suas.
  - `PUT /fichas/{id}/descricao`;
  - `POST /fichas/{id}/habilidades`;
  - `PUT` e `DELETE /fichas/{id}/habilidades/{habilidadeId}`.
- **Fora do `PUT` da ficha:** o corpo do `PUT /fichas/{id}` não leva descrição nem habilidades (se levar, o JSON estrito responde 400), e o formulário não as envia nem as apaga.
- **A `Ficha` não tem coleção de habilidades:** elas ficam na entidade `HabilidadeFicha`, com `@ManyToOne` para a ficha, e são lidas pelo `HabilidadeFichaRepository`. A descrição (aparência, história e anotações) são três colunas novas da tabela `ficha`.
- **O detalhe traz tudo:** o `GET /fichas/{id}` devolve `descricao` e `habilidades`, montadas pelo `PartesComunsDaFicha`. A listagem não chama esse componente e continua em 1 consulta.
- **Item de outra ficha é 404:** pedir uma habilidade que não existe, ou que é de outra ficha, responde 404 "Item não encontrado". A exceção é genérica (`ItemDaFichaNaoEncontradoException`, com o nome da lista) e as próximas listas a reaproveitam.

## Alternativas consideradas
- **Listas dentro do `PUT` da ficha:** o contrato ficaria único, mas cada mudança reenviaria a ficha inteira, o formulário ficaria enorme e uma aba poderia sobrescrever o que a outra acabou de salvar.
- **JSONB:** fácil de mudar, mas o banco deixaria de garantir o tipo e o tamanho de cada campo (ver [ADR 0015](0015-uma-tabela-por-sistema.md)).
- **Coleção JPA na `Ficha` com `orphanRemoval`:** parece natural, mas a listagem e o `PUT` passariam a carregar e gerenciar as listas, e o teste das consultas (`ListagemSemNMaisUmTest`) deixaria de valer sem ajustes.

## Consequências
- Cada aba salva sozinha, sem conflito com a edição rápida (`PATCH`) nem com o formulário.
- O tipo da habilidade vale por sistema (`TipoHabilidade.permitidoEm`); um tipo de outro sistema responde 400 em `errors.tipo`. A `check` do banco aceita todos os tipos, porque a tabela não sabe o sistema da ficha.
- Uma lista nova repete um padrão conhecido: tabela, entidade, repositório, serviço e controller, mais o componente `ListaEditavel` no front.
- **Pior:** há mais rotas e mais controllers para manter.
- **Pior:** o "PUT = substituição completa" do ADR 0008 passa a valer só para os campos centrais da ficha. Quem lê a API precisa saber que descrição e listas têm rotas à parte.
