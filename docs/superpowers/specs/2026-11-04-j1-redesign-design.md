# J1 fase 2: redesign do frontend, retrato e dado 3D (design)

- **Status:** aprovado em conversa, parte por parte, pelo Arthur em 2026-11-04. Aguarda a revisão desta spec escrita.
- **Base:** `main` em `d25377b`, com o 2.1 mesclado (Angular 22.2, Material 22.2, 57 testes no front, 74 no back, ADRs 0001–0013).
- **Relação com a spec da fase 2** (`2026-11-04-j1-fase-2-design.md`): esta spec **substitui o §7 (layout no computador)** e **traz o upload de imagem para dentro do escopo**. O resto da fase 2 (login, campanhas, tempo real, Google, deploy) não muda.

## 1. Objetivo

O Arthur não gostou do 2.1 no computador, que mostra dois painéis lado a lado. Os pedidos:
1. um **painel (dashboard)** com as fichas e uma **tela própria** para cada ficha;
2. um visual **mais bonito e moderno**;
3. o jogador **escolhe a imagem de perfil** do personagem;
4. uma rolagem **mais visual**, com o dado sendo rolado na tela.

**Escolhas do Arthur:**
- **visual:** "Moderno com alma gótica", ou seja, a organização de um app moderno (busca, filtros, blocos) com a aparência de um grimório (névoa púrpura, dourado, serifa, retrato em medalhão). Foi a versão 1 da mistura entre as direções "Grimório" e "Moderno premium";
- **imagem:** upload próprio;
- **dado:** 3D com física.

**Sucesso:**
- no computador e no celular, o painel mostra as fichas como cartões com retrato, e tocar num cartão abre a ficha numa tela própria;
- o jogador troca o retrato em menos de 30 s, sem precisar editar a imagem fora do app;
- ao rolar uma perícia, dois d10 caem na tela mostrando exatamente o número que o servidor sorteou;
- o pacote inicial continua leve (a parte 3D só é baixada na primeira rolagem).

**Fora do escopo:**
- galeria de imagens prontas, imagens de fundo da ficha e geração de imagem;
- outros dados além do d100 (o sistema só usa d100);
- som da rolagem;
- histórico de rolagens (continua no 2.4);
- preferência de "animação ligada/desligada" no app (respeitamos a preferência de movimento reduzido do sistema).

## 2. Incrementos

Cada incremento passa pelo fluxo da fase 2: dev Sonnet → verificador Sonnet separado → documentação → PR com o CI verde, mesclado como *merge commit*. A autorização do Arthur para os PRs da fase 2 vale aqui. Todos ficam **antes do 2.2 (login)**.

| Inc. | Entrega | Pronto quando |
|---|---|---|
| **2.1b** | Novo visual (§3): tema, topo novo, painel e ficha em blocos, formulário com o tema novo. Só frontend; API sem mudança. | O fluxo completo (painel → ficha → rolar → editar → excluir → criar) funciona a 375 px e a 1280 px. Os testes unitários ficam verdes, `ng build` passa no CI dentro do orçamento e as cores de texto passam no contraste AA. |
| **2.1c** | Retrato (§4): migração V3, rotas do retrato, `retratoVersao` no JSON, recorte no navegador e medalhão com retrato ou iniciais. Mais a **edição rápida de vida e sanidade** na ficha (§4.6), pedida pelo Arthur depois de ver o 2.1b. | Os testes de integração da §4.5 ficam verdes, o recorte e a edição rápida têm testes unitários, e trocar e remover o retrato e ajustar vida e sanidade funcionam no navegador a 375 px e a 1280 px. |
| **2.1d** | Dado 3D (§5): spike, sobreposição com dois d10 forçados, plano B automático e toque para pular. | A conversão e a escolha entre 3D e plano B têm testes. A rolagem é conferida a 375 px e a 1280 px com captura de tela, e o build mostra a biblioteca 3D num pedaço separado do pacote inicial. |

**Ordem revista pelo Arthur em 2026-11-04**, depois de ver o 2.1b: 2.1c → **vários sistemas** (spec `2026-11-04-j1-sistemas-design.md`: 2.1d Ordem Paranormal, 2.1e D&D 5e e 2024; o d100 sai) → **2.1f dado animado**. O dado vem depois dos sistemas porque precisa do d20 (D&D) e de vários d20 (Ordem Paranormal). A §5 abaixo vale como histórico: o dado é revisto num design curto antes do 2.1f, já sem o d100.

**Novos ADRs** (próximos números livres a partir de 0014):
- retrato no PostgreSQL, numa tabela separada (alternativa: armazenamento de objetos, reavaliado no 2.6);
- dado 3D com biblioteca e resultado forçado pelo servidor.

## 3. Telas e visual (2.1b)

### 3.1 Navegação

| Rota | Tela |
|---|---|
| `/fichas` | Painel |
| `/fichas/nova` | Formulário (criar) |
| `/fichas/:id` | Ficha |
| `/fichas/:id/editar` | Formulário (editar) |

- **Topo fixo** em todas as telas: marca "✦ Grimório" (leva ao painel) e "+ Nova ficha". A busca fica no próprio painel, na linha dos filtros, logo abaixo dos contadores.
- **Saem:** a coluna lateral com a lista (computador) e a navegação de baixo (celular), incluindo as abas "Mesa · Minha ficha · Rolagens" previstas na spec da fase 2. O serviço `Layout` sai se nada mais o usar.
- **Rolagens ao vivo (2.4):** não voltam a ser uma coluna fixa. A preferência é um painel lateral que abre por um botão, decidido no 2.4.
- **Campanhas (2.3):** a tela da campanha reaproveita o painel, filtrado pela campanha.

### 3.2 Painel

- **Contadores:** investigadores, feridos e sanidade baixa.
- **Filtros:** Todos · Feridos · Sanidade baixa.
- **Regras:**
  - **ferido** = vida atual ≤ metade da vida máxima;
  - **sanidade baixa** = sanidade atual ≤ metade da sanidade máxima;
  - uma ficha sem o valor atual ou sem o máximo (ou com máximo 0) não entra nesses filtros, e a barra dela mostra "—".
- **Busca:** por personagem ou jogador, sem diferenciar maiúsculas e acentos.
- **Cartão:** medalhão (retrato ou iniciais), nome em versalete, jogador e idade, barras de PV e SAN com os números, e uma etiqueta "Ferido" ou "Sanidade baixa" quando for o caso. O cartão inteiro é um link para a ficha. A ficha não tem campo de ocupação (os mockups usavam "Detetive" só como ilustração); acrescentá-lo mudaria o contrato e fica fora deste redesign.
- **Fonte dos dados:** tudo acontece no navegador, sobre a lista que `GET /fichas` já devolve, sem rota nova. É simples e basta enquanto a lista é pequena; a paginação no servidor fica para quando houver muitas fichas por campanha.
- **Estados:** carregando (cartões esqueleto), vazio ("Nenhuma ficha ainda" + criar), erro com "Tentar de novo" e "Servidor indisponível" quando o status é 0, como já existe hoje.
- **Grade:** 1 coluna no celular, 2 a partir de 600 px e 3 a partir de 900 px.

### 3.3 Ficha

**Blocos:**
1. Retrato + nome + jogador · idade · gênero + "Trocar imagem" (o botão só entra no 2.1c).
2. Anéis de vida e de sanidade, com os números escritos ao lado (o anel não é a única forma de ler o valor).
3. Atributos em grade 3×3.
4. Perícias: as 23 do catálogo, na ordem do catálogo. Cada uma mostra uma barra até o valor, a marca do sucesso extremo em ⌊valor/5⌋, o valor e o botão "Rolar". As perícias com valor 0 ficam esmaecidas, mas podem ser roladas (a regra conta 0).

**Ações:** Editar e Excluir ficam num menu ⋮ no topo da ficha. Excluir continua pedindo confirmação.

**Layout:**
- no computador, o retrato fica à esquerda ocupando duas linhas, os anéis e os atributos à direita, e as perícias embaixo, em largura total (duas colunas de lista a partir de 900 px);
- no celular, os blocos ficam empilhados nessa ordem.

**Resultado da rolagem:** o componente `resultado-rolagem` continua mostrando as faixas (extremo, sucesso, falha) e o número. No 2.1d ele aparece depois da animação.

As dicas "ⓘ" de atributos e perícias continuam só nos botões pequenos (correção do 2.1: o tooltip bloqueia a rolagem da página no toque).

### 3.4 Formulário

A lógica do 2.1 fica igual: `paraEntrada`, campos vazios, carregamento antes de editar, foco no primeiro inválido, bloqueio de envio duplo e erro visível perto do Salvar. Muda só a aparência: seções em blocos com título dourado e campos do Material com o tema novo.

### 3.5 Visual

**Tokens** (variáveis CSS, substituindo os do 2.1):

| Token | Valor | Uso |
|---|---|---|
| `--cor-fundo` | `#0b0a0f` + névoa púrpura (`radial-gradient` com `rgba(88,52,110,.38)` no topo) | fundo |
| `--cor-superficie` | `#121019` | blocos e cartões |
| `--cor-borda` | `#262031` | borda comum |
| `--cor-borda-dourada` | `rgba(201,164,92,.42)` | filete dos cartões e do retrato |
| `--cor-texto` / `--cor-texto-suave` | `#e6e1d6` / `#8a8296` | texto |
| `--cor-titulo` | `#f0e2bf` | nomes e títulos |
| `--cor-acao` | `#c9a45c` | botões e destaques |
| `--cor-vida` / `--cor-sanidade` | `#e0604b` / `#57c49a` | barras e anéis |

**Fontes:**
- títulos e nomes em **EB Garamond** (licença OFL), com `font-variant-caps: small-caps` nos nomes;
- a fonte vem do pacote npm `@fontsource/eb-garamond` e é servida pelo próprio app (sem CDN externa e sem custo), só com os pesos 400 e 600 e o subconjunto latino;
- o texto corrido continua com a fonte do sistema.

**Medalhão:** oval 4:5, aro dourado de 1,5–2 px e um anel externo translúcido. Sem retrato, mostra as iniciais (até 2 letras) sobre um degradê escolhido pelo id da ficha, para que a mesma ficha tenha sempre a mesma cor.

**Material:** continua sendo a base dos campos, diálogos, menus e snackbars, com os tokens do tema M3 escuro sobrescritos pelas cores acima.

**Acessibilidade:**
- contraste AA nos textos;
- foco visível em dourado;
- o "Rolar" tem `aria-label` com a perícia e o valor (ex.: "Rolar Investigação, 70");
- o retrato tem `alt` "Retrato de {personagem}";
- os alvos de toque têm pelo menos 44 px.

### 3.6 Testes do 2.1b

- **Testes que mudam de componente:** os componentes que saem (`ficha-lista`, `ficha-inicio`, a coluna do `shell`) têm comportamentos que continuam existindo (carregar a lista, erro, vazio, link para a ficha). Os testes deles **migram** para o painel. Nenhum comportamento perde teste, e a lista de testes migrados vai no PR.
- **Testes novos:** regras de ferido e sanidade baixa, incluindo os valores ausentes e o máximo 0; busca sem acento; contadores; filtro; iniciais e cor estável do medalhão; marca do extremo (⌊valor/5⌋).

## 4. Retrato do personagem (2.1c)

### 4.1 Fluxo no navegador

1. Na ficha, o jogador toca em "Trocar imagem" e escolhe um arquivo (`accept="image/*"`).
2. Abre um diálogo com a imagem dentro de uma moldura 4:5. Ele **arrasta** para posicionar e usa um **controle de zoom**; a imagem sempre cobre a moldura inteira.
3. Em "Salvar", o app recorta e reduz a imagem num `canvas` para **400×500**, exporta em **JPEG com qualidade 0,85** (cerca de 30–60 KB) e envia.
4. "Remover imagem" pede confirmação e volta para as iniciais.

O recorte é feito pelo próprio app, sem biblioteca. A conta "posição + zoom → retângulo de origem" é uma função pura, testada à parte. O motivo é evitar uma dependência que pode não acompanhar o Angular 22, e o código cabe em poucas dezenas de linhas.

Um arquivo que o navegador não consegue abrir (ex.: HEIC em alguns navegadores) mostra "Não foi possível abrir esta imagem; tente JPG ou PNG".

### 4.2 API

| Rota | Corpo / resposta | Erros |
|---|---|---|
| `PUT /fichas/{id}/retrato` | corpo: os bytes da imagem, `Content-Type: image/jpeg`; resposta **204** | 404 ficha inexistente; 415 tipo diferente de JPEG; 413 acima de 200 KB; 422 imagem inválida ou acima de 1000 px por lado |
| `GET /fichas/{id}/retrato` | `200 image/jpeg`, `Cache-Control: private, max-age=31536000, immutable`, `X-Content-Type-Options: nosniff` | 404 ficha inexistente ou sem retrato |
| `DELETE /fichas/{id}/retrato` | **204** (também quando a ficha não tem retrato) | 404 ficha inexistente |

Tudo em ProblemDetail, como hoje.

**JSON da ficha:**
- ganha `retratoVersao` (número, ou `null` sem retrato), que é o instante do último envio em milissegundos (`atualizado_em`). Ele muda a cada `PUT` e volta a `null` no `DELETE`;
- não pode ser um contador: remover e enviar de novo recomeçaria em 1, e o navegador mostraria a imagem antiga guardada em cache;
- só aparece na **resposta**: o front nunca o envia, e o JSON estrito continua recusando chaves desconhecidas na entrada;
- o instante vem de um `Clock` injetável (como o `Dado` do ADR 0011) e é truncado em milissegundos. Se o novo envio cair no mesmo milissegundo (ou antes) do retrato atual, a versão nova é a anterior + 1 ms, para nunca se repetir. Nos testes, o relógio é controlado.

### 4.3 Validação no servidor

O servidor não confia no navegador:
- `Content-Type` precisa ser `image/jpeg`;
- o corpo tem no máximo 200 KB (`Content-Length` conferido e leitura limitada, para não carregar um corpo gigante em memória);
- os bytes começam com `FF D8 FF`;
- a imagem precisa decodificar com `ImageIO` e ter no máximo 1000 px de largura e de altura.

Formatos como SVG nunca são aceitos, porque podem carregar script. A resposta do `GET` leva `nosniff` para o navegador não "adivinhar" outro tipo.

### 4.4 Banco (migração V3)

```sql
create table retrato_ficha (
  ficha_id      bigint primary key references sheet_player(id) on delete cascade,
  conteudo      bytea not null,
  atualizado_em timestamptz not null
);
```

- A tabela é **separada** da ficha, para a listagem nunca ler bytes de imagem. `retratoVersao` vem de uma junção ou consulta leve, sem carregar `conteudo`.
- Excluir a ficha apaga o retrato (`on delete cascade`).
- No 2.3, a referência acompanha a renomeação de `sheet_player` para `ficha`.
- **Espaço:** cerca de 50 KB por retrato, o que dá milhares de retratos nos 0,5 GB do Neon gratuito.

**Exibição no front:**
- o front baixa o retrato pelo `HttpClient` como `blob` e cria uma URL local (`URL.createObjectURL`). Não usa `<img src>` apontando para a API: a partir do 2.2 a rota exige token, e o `<img>` não envia o cabeçalho `Authorization`;
- um serviço guarda uma URL por ficha e versão, e revoga a URL antiga quando a versão muda;
- a URL `/retrato?v={versao}` deixa o cache HTTP do navegador funcionar sem mostrar imagem velha.

### 4.5 Testes do 2.1c

**Backend** (integração com Testcontainers):
- `PUT` de um JPEG válido → 204, `GET` devolve os mesmos bytes e os cabeçalhos, e a ficha passa a ter `retratoVersao`;
- um segundo `PUT` → `retratoVersao` diferente do primeiro, e o mesmo vale para remover e enviar de novo;
- PNG ou SVG → 415;
- acima de 200 KB → 413;
- bytes que não são imagem e imagem acima de 1000 px → 422;
- ficha inexistente → 404 nas três rotas;
- `GET` sem retrato → 404;
- `DELETE` → `retratoVersao` volta a `null`;
- excluir a ficha apaga o retrato;
- teste de migração V3 com as fichas existentes (`retratoVersao` = `null`).

**Frontend:**
- a conta do recorte em vários tamanhos e proporções de imagem e com zoom no limite;
- o serviço de retrato (cria e revoga as URLs);
- o diálogo (salvar envia JPEG, cancelar não envia);
- o medalhão (retrato, ou iniciais quando não há);
- a edição rápida (§4.6).

### 4.6 Edição rápida de vida e sanidade

Pedido do Arthur ao ver o 2.1b: mudar a vida e a sanidade **atuais** sem abrir o formulário.

- **Onde:** no bloco de vida e sanidade da ficha. Cada anel ganha um botão **−** e um **+** (44 px, 1 ponto por toque), com os nomes acessíveis "Diminuir vida", "Aumentar vida", "Diminuir sanidade" e "Aumentar sanidade". Tocar no número abre um campo para **digitar o valor** (Enter confirma, Esc cancela). Os máximos continuam só no formulário.
- **Limites:** o valor atual fica entre 0 e o máximo. Sem máximo, o único limite é "no mínimo 0".
- **Salvamento:**
  - a tela muda na hora e salva sozinha **0,8 s depois do último toque**, então vários toques seguidos viram um salvamento só;
  - usa a rota que já existe (`PUT /fichas/{id}` com a ficha inteira), **sem mudar a API**. Com os vários sistemas, cada um terá recursos próprios (PV, SAN, PE, pontos de vida…), e uma rota dedicada nasce lá;
  - o corpo do `PUT` sai da ficha carregada, sem `id` e sem `retratoVersao` (o JSON estrito recusaria);
  - durante o salvamento aparece um "salvando…" discreto. Se falhar, o valor volta ao último salvo e o erro aparece;
  - um toque durante um salvamento agenda outro salvamento depois dele, e a resposta do primeiro não apaga o toque novo.
- **Painel:** o `versaoFichas` muda depois do `PUT`, então o painel já mostra o valor novo ao voltar.
- **Testes:** limites de 0 ao máximo; vários toques viram um `PUT` (com relógio simulado); erro volta o valor; toque durante o salvamento; digitar o valor; nomes acessíveis.

## 5. Dado 3D (agora 2.1f; ver a spec dos sistemas)

### 5.1 Biblioteca

**Escolhida:** `open-dice-dnd`.
- Licença MIT; usa `three` e `cannon-es` como dependências.
- Permite **forçar o valor de cada dado**, que é o essencial: o servidor continua sendo quem sorteia (ADR 0011).
- Representa o d100 como dois d10 (dezenas e unidades) e não exige arquivos públicos extras.

**Descartadas** (pesquisa de 2026-11-04):
- `@3d-dice/dice-box`: cerca de 11 MB e sem resultado forçado documentado;
- `threejs-dice` e `dice-box-threejs`: sem manutenção;
- `react-ttrpg-dice`: só funciona com React.

**Spike** (primeira tarefa do 2.1d, com o código descartável). Confirma:
1. que a biblioteca compila com Angular 22 (builder com esbuild/Vite);
2. que os dois d10 param nos valores forçados;
3. o tamanho do pedaço carregado sob demanda;
4. que funciona no Chrome com emulação de celular.

**Se o spike falhar,** o Arthur é avisado e a proposta passa a ser dois d10 animados em 2D (SVG + CSS) no mesmo fluxo.

### 5.2 Fluxo

1. "Rolar" → o botão mostra "rolando…" e o app faz o `POST /fichas/{id}/testes-de-pericia` como hoje.
2. Com a resposta, abre uma camada transparente sobre a ficha, e dois d10 são lançados com os valores forçados (cerca de 1,5 s).
3. Quando os dados param, aparece o `resultado-rolagem` com perícia, valor, rolagem e faixa.
4. Um toque na camada, ou a tecla Esc, pula a animação e mostra o resultado na hora.
5. Se o `POST` falhar, nenhum dado é lançado e o erro aparece como hoje.

**Conversão (função pura):** a rolagem `n` (1–100) vira `dezena = ⌊(n mod 100)/10⌋ × 10` (mostrada como 00–90) e `unidade = n mod 10`.

| Rolagem | Dezena | Unidade |
|---|---|---|
| 1 | 00 | 1 |
| 23 | 20 | 3 |
| 70 | 70 | 0 |
| 100 | 00 | 0 |

**Carregamento:** a biblioteca e o `three` só são importados na **primeira rolagem** (`import()` dinâmico) e não entram no pacote inicial.

### 5.3 Plano B automático

O app mostra o resultado direto, sem a camada 3D, quando:
- o sistema pede movimento reduzido (`prefers-reduced-motion: reduce`);
- não há WebGL;
- o `import()` ou a criação da cena falha.

A falha vai só para o console, porque o jogador não precisa saber: a rolagem em si funcionou.

### 5.4 Testes do 2.1d

- **Unitários:** a conversão (tabela acima, mais 10, 99 e 100); a escolha entre 3D e plano B nos três casos; o fluxo (o resultado só aparece depois de a animação terminar ou ser pulada; falha no `POST` não lança dados). A camada 3D fica atrás de uma interface (`LancadorDeDados`), então os testes usam uma versão falsa.
- **Manual com evidência:** capturas a 375 px e a 1280 px com os dados parados no valor certo, e a saída do build mostrando o pedaço separado.

## 6. Riscos

| Risco | Mitigação |
|---|---|
| `open-dice-dnd` não funcionar com Angular 22 ou não forçar o resultado | Spike no início do 2.1d; plano B em 2D já definido |
| Cena 3D pesada em celular fraco | Carregamento sob demanda, toque para pular, plano B automático com movimento reduzido |
| Imagens enchendo o banco gratuito | Limite de 200 KB (na prática, cerca de 50 KB), tabela separada, reavaliação no 2.6 |
| Fonte nova pesando no carregamento | Só 2 pesos e o subconjunto latino, servidos pelo app |
| Imagem maliciosa | Só JPEG, bytes iniciais conferidos, decodificação no servidor, `nosniff` |
| Redesign quebrar comportamentos do 2.1 | Os testes migram junto com os componentes; nenhum comportamento perde teste |
