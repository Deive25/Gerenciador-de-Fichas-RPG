# 0024. Permissões de campanha num ponto central

Status: aceita · Incremento 2.3 · 2026-11-16

## Contexto
Até o 2.2, cada pessoa só alcançava as próprias fichas, e um componente único (`FichasDoUsuario`) buscava a ficha por id **e** dono. O 2.3 traz as campanhas: o mestre precisa ver, editar e rolar com as fichas da mesa, e o jogador precisa ver o resumo das fichas dos outros jogadores sem mexer nelas. A spec da fase 2 já tinha fixado o 404 para quem está fora da campanha e o 403 para o membro sem permissão. Há 12 serviços que leem ou gravam dados da ficha (itens, magias, rituais, ataques, retrato, rolagens...).

## Decisão
- **A regra fica num componente só, o `AcessoAFicha`** (o `FichasDoUsuario` renomeado), com uma ordem fixa:
  1. a ficha não existe → 404;
  2. quem está logado é o dono → libera;
  3. a ficha está sem campanha → 404;
  4. não é membro da campanha da ficha → 404;
  5. é o mestre → libera (menos excluir e mover, que são só do dono: 403);
  6. é jogador e a ficha é do mestre → 404 (os NPCs ficam ocultos);
  7. é jogador e a ficha é de outro jogador → 403.
- Quatro métodos cobrem os casos: `buscar` e `exigir` (dono ou mestre), `exigirDono` (excluir e trocar de campanha) e `exigirVisivel` (só o `GET` do retrato, que o cartão do resumo mostra a qualquer membro). Os 12 serviços só trocaram o nome da dependência. As campanhas têm a regra irmã, `AcessoACampanha` (`exigirMembro`, `exigirMestre`).
- **A ficha pode ficar sem campanha** (escolha do Arthur). A campanha "Fichas importadas" da spec da fase 2 saiu, porque as fichas antigas já eram do Arthur desde o 2.2.
- **Só o dono exclui a ficha** (escolha do Arthur). Com a ficha podendo viver fora da campanha, ela é do jogador, e excluir é irreversível. O mestre que não quer uma ficha na mesa remove o jogador.
- **O banco garante "a ficha só fica numa campanha da qual o dono participa"** com uma chave estrangeira composta `(campanha_id, dono_id)` → `participacao (campanha_id, usuario_id)`, com `on delete set null (campanha_id)`. Sair, ser removido ou apagar a campanha (que apaga as participações em cascata) devolve as fichas a "sem campanha" sem código no serviço. O índice parcial `ux_participacao_um_mestre` garante no máximo um mestre por campanha.

## Alternativas consideradas
- **`@PreAuthorize("@acesso.podeEditar(#id)")` em cada rota:** é o caminho idiomático do Spring e deixa a regra visível em cima da rota, mas seriam cerca de 30 métodos anotados em 12 controllers, a ficha seria carregada duas vezes (na checagem e no serviço), e o 403 padrão precisaria virar 404 para quem está fora.
- **Row-Level Security no PostgreSQL:** o banco garantiria o acesso mesmo se o código errasse, mas exige passar o usuário em cada transação, convive mal com o pool de conexões e não distingue 403 de 404. É demais para o tamanho do projeto.
- **FK simples para `campanha` e a regra no serviço:** funciona, mas cada caminho que remove alguém precisaria lembrar de soltar as fichas dele. A FK composta faz isso num lugar só.

## Consequências
- A matriz inteira é testada num ponto só (`PermissoesDaFichaTest`, `PermissoesDaCampanhaTest`), em leitura, escrita, listas, retrato, exclusão e troca de campanha.
- **Pior:** a regra vive no Java. Um serviço novo que buscasse a ficha direto no `FichaRepository` fugiria dela. Hoje o repositório só é usado no `AcessoAFicha` e no `FichaService`, e o verificador procura outros usos a cada incremento.
- **Pior:** o `on delete set null` de uma coluna só existe no PostgreSQL 15 ou mais novo. O projeto usa o 17 no compose, no Testcontainers e no Neon.
- **Pior:** quem apaga a participação não pode carregar as fichas na mesma transação, senão o Hibernate poderia regravar o `campanha_id` antigo. O serviço só apaga e dá `flush`, e um teste confere que o `PUT` seguinte da ficha funciona.
- O link de convite vazado deixa entrar quem o tiver; o mestre renova o código (o antigo para na hora) e remove quem entrou. O limite de consultas ao convite fica para o deploy (2.6).
