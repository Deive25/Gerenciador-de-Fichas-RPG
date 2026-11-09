# J1 fase 2: vários sistemas de RPG — Ordem Paranormal, D&D 5e e D&D 2024 (design)

- **Status:** aprovado em conversa, parte por parte, pelo Arthur em 2026-11-04. Aguarda a revisão desta spec escrita.
- **Base:** `main` em `d2a401a` (2.1c mesclado). Backend com 98 testes, frontend com 138, ADRs 0001–0014.
- **Relação com outras specs:**
  - esta spec **substitui o sistema d100** que o app tem desde 2021;
  - ela **renumera o incremento do dado animado** da spec do redesign (`2026-11-04-j1-redesign-design.md` §5), que vira o 2.1f e passa a rolar d20;
  - também **antecipa a renomeação** `sheet_player` → `ficha`, que a spec da fase 2 previa no 2.3.

## 1. Objetivo

O grupo do Arthur joga Ordem Paranormal e D&D. O app passa a criar e usar fichas desses sistemas.

**Escolhas do Arthur:**
- **Sistemas:** Ordem Paranormal (livro básico: PV, SAN e PE, com NEX e classe), D&D 5e (2014) e D&D 2024 (o "One D&D").
- **O sistema d100 sai.** As fichas que já existem viram Ordem Paranormal (§4).
- **Escolha do sistema:** feita **na criação da ficha**, e não muda depois.
- **Profundidade:** **só o núcleo jogável**, ou seja, atributos, perícias com treino, recursos, nível/NEX e a rolagem com os dados certos.
- **D&D:** tem **vantagem e desvantagem**.
- **Resultado:** **só o total**, sem CD/DT, porque quem compara é o mestre, como na mesa.
- **Armazenamento:** **abordagem C**, uma tabela por sistema, escolhida entre três (§3).

**Sucesso:**
- criar uma ficha de cada sistema, editar, rolar perícia e atributo com o dado certo e ajustar os recursos na edição rápida, a 375 px e a 1280 px;
- as fichas antigas continuam no banco, convertidas para Ordem Paranormal, sem perder nome, jogador, idade, retrato, vida e sanidade.

**Fora do escopo:**
- magias e rituais, inventário, ataques e dano, habilidades de classe e de trilha;
- cálculo automático de PV/SAN/PE/PV máximos (o jogador digita);
- CD/DT;
- trocar o sistema de uma ficha existente;
- outros sistemas.

## 2. Incrementos

Mesmo fluxo da fase 2: TDD → verificador separado → documentação → PR com o CI verde, mesclado como *merge commit*. A autorização do Arthur para os PRs da fase 2 vale aqui.

| Inc. | Entrega | Pronto quando |
|---|---|---|
| **2.1d** | O esqueleto novo e **Ordem Paranormal completa**: migração V4 (`ficha` + `ficha_ordem` e conversão das fichas antigas), API polimórfica, `PATCH` de recursos, rolagem com vários d20, escolha do sistema na criação (D&D aparece como "em breve"), formulário, ficha e painel de Ordem. **O d100 sai.** | Os testes de migração, API e regras ficam verdes. O fluxo completo de Ordem (criar → ficha → rolar → edição rápida → editar → excluir) funciona a 375 px e a 1280 px. As fichas antigas aparecem convertidas. |
| **2.1e** | **D&D 5e e 2024** no mesmo esqueleto: `ficha_dnd`, regras com proficiência, especialização e vantagem/desvantagem, e formulário e ficha de D&D. | Testes das regras nas fronteiras (modificador, proficiência por nível, especialização, vantagem); fluxo completo de D&D nas duas larguras. |
| **2.1f** | **Dado animado** (era o 2.1d da spec do redesign), já com d20: vários dados em Ordem, 2 com vantagem/desvantagem e o dado mantido destacado. | Design curto aprovado em 2026-11-09: spec do redesign §5 (revista). |

**Novos ADRs:**
- **0015**, "uma tabela por sistema (herança JOINED)", com as alternativas de catálogo + tabelas genéricas e de JSONB;
- **0016**, "saída do d100 e conversão das fichas para Ordem Paranormal".

## 3. Modelo de dados

**Abordagem C** (escolha do Arthur): uma tabela comum e uma tabela por sistema, ligadas 1:1. No Java, é herança JPA `@Inheritance(strategy = JOINED)`: `Ficha` abstrata, mais `FichaOrdem` e, no 2.1e, `FichaDnd`.

**Alternativas avaliadas** (vão para o ADR 0015):
- **A · catálogo por sistema + tabelas genéricas** (`atributo`, `pericia`, `recurso` por código): menos migrações por sistema novo, mas o banco não conhece as regras;
- **B · JSONB por sistema**: poucas tabelas, mas o banco vira depósito de JSON.

A C dá o modelo mais tipado e com restrições no banco. O custo é uma migração, uma entidade e um DTO por sistema novo.

### 3.1 `ficha` (comum; substitui `sheet_player`)

| Coluna | Tipo | Regra |
|---|---|---|
| `id` | bigint identity | PK |
| `sistema` | varchar(30) | `ORDEM_PARANORMAL` ou `DND` (check) |
| `jogador`, `personagem`, `genero` | varchar(255) | obrigatórios |
| `idade` | integer | opcional |

`retrato_ficha.ficha_id` passa a referenciar `ficha(id)`, com `on delete cascade`.

### 3.2 `ficha_ordem` (2.1d)

| Coluna | Tipo | Regra |
|---|---|---|
| `ficha_id` | bigint | PK, FK → `ficha` on delete cascade |
| `classe` | varchar(20) | `COMBATENTE`, `ESPECIALISTA`, `OCULTISTA` ou nulo ("a definir") |
| `origem` | varchar(255) | texto livre, opcional |
| `nex` | integer | 5 a 99, obrigatório |
| `agilidade`, `forca`, `intelecto`, `presenca`, `vigor` | integer | 0 a 5, obrigatórios |
| `pv_atual`, `pv_max`, `san_atual`, `san_max`, `pe_atual`, `pe_max` | integer | ≥ 0, opcionais |

`ficha_ordem_pericia (ficha_id FK on delete cascade, pericia varchar(30), grau varchar(10))`:
- a PK é `(ficha_id, pericia)`;
- o `grau` é `TREINADO`, `VETERANO` ou `EXPERT`;
- quando não há linha, a perícia está destreinada.

### 3.3 `ficha_dnd` (2.1e)

| Coluna | Tipo | Regra |
|---|---|---|
| `ficha_id` | bigint | PK, FK → `ficha` on delete cascade |
| `edicao` | varchar(5) | `D2014` ou `D2024` |
| `classe`, `especie`, `antecedente` | varchar(255) | texto livre, opcional (o rótulo de `especie` é "Raça" em 2014 e "Espécie" em 2024) |
| `nivel` | integer | 1 a 20 |
| `forca`, `destreza`, `constituicao`, `inteligencia`, `sabedoria`, `carisma` | integer | 1 a 30 |
| `pv_atual`, `pv_max`, `pv_temporario` | integer | ≥ 0, opcionais |
| `ca` | integer | ≥ 0, opcional |
| `inspiracao` | boolean | padrão falso |
| `salva_forca` … `salva_carisma` | boolean | proficiência em cada salvaguarda |

`ficha_dnd_pericia (ficha_id, pericia, grau)`, com o `grau` sendo `PROFICIENTE` ou `ESPECIALISTA`.

## 4. Migração V4 (2.1d): o d100 sai e as fichas viram Ordem Paranormal

Tudo numa transação (o PostgreSQL tem DDL transacional). **Nenhuma ficha é apagada.**

1. Renomeia `sheet_player` → `ficha` e as colunas de identificação para os nomes claros (`namej` → `jogador`, `namec` → `personagem`, `gender` → `genero`, `age` → `idade`). Acrescenta `sistema`, preenchido com `ORDEM_PARANORMAL`.
2. Cria `ficha_ordem` e `ficha_ordem_pericia`.
3. Converte cada ficha:
   - **mantém** vida → PV (`pv_atual` = `lifea`, `pv_max` = `lifem`) e sanidade → SAN (`san_atual` = `sana`, `san_max` = `sanm`);
   - **deixa vazios** PE (`pe_atual` e `pe_max`), `classe` e `origem`;
   - **usa padrões:** `nex` = 5 e os 5 atributos = 1 (valor inicial do livro; 0 mudaria a regra da rolagem);
   - **perícias:** começam destreinadas. As escalas d100 e d20 não se convertem, e é decisão do Arthur.
4. Remove as colunas d100 de `ficha` (`lifea`/`lifem`/`sana`/`sanm` e os 9 atributos antigos) e a tabela `ficha_pericia`.
5. `retrato_ficha` continua com as mesmas linhas; a FK já aponta para a tabela renomeada.

**Teste de migração**, no padrão do `MigracaoPericiasTest`: migra até a V3, insere fichas d100 (com perícias e com retrato), migra até a V4 e confere:
- cada ficha, com nome, jogador, gênero, idade, PV e SAN;
- `sistema` = `ORDEM_PARANORMAL`, NEX 5 e atributos 1;
- nenhuma perícia;
- o retrato intacto;
- as colunas e a tabela antigas ausentes.

## 5. API

As regras atuais continuam: JSON estrito, ProblemDetail em português e `PUT` como substituição completa.

### 5.1 Fichas

| Rota | O que muda |
|---|---|
| `GET /fichas` | Devolve o **resumo** de cada ficha: `id`, `sistema`, `jogador`, `personagem`, `idade`, `retratoVersao` e `recursos` (a lista dos recursos principais, como no exemplo). O painel não precisa conhecer os detalhes do sistema. |
| `GET /fichas/{id}` | Devolve a ficha completa, com o campo `sistema` como discriminador. |
| `POST /fichas` | Recebe o `sistema` no corpo e escolhe o DTO certo. Sistema desconhecido → 400. |
| `PUT /fichas/{id}` | Mesmo corpo do `POST`. Um `sistema` diferente do da ficha → **409** "O sistema de uma ficha não muda". |
| `DELETE /fichas/{id}` | Sem mudança. |
| `PATCH /fichas/{id}/recursos` | **Novo.** Recebe só os valores **atuais**: `{ "PV": 10, "SAN": 30 }`. Os códigos aceitos dependem do sistema (Ordem: `PV`, `SAN`, `PE`; D&D: `PV`, `PV_TEMP`). Código de outro sistema → 400. Valor < 0 → 400. Devolve a ficha completa. É a rota prometida na spec do redesign §4.6, e a edição rápida passa a usá-la no lugar do `PUT`. |
| `POST /fichas/{id}/testes` | **Nova**, substitui `/testes-de-pericia` (§5.2). |

Exemplo da ficha completa de Ordem:

```json
{
  "id": 7, "sistema": "ORDEM_PARANORMAL",
  "jogador": "Ana", "personagem": "Jack Moreau", "genero": "M", "idade": 38, "retratoVersao": null,
  "classe": "ESPECIALISTA", "origem": "Investigador", "nex": 10,
  "atributos": { "AGI": 2, "FOR": 1, "INT": 3, "PRE": 2, "VIG": 1 },
  "recursos": { "PV": { "atual": 18, "maximo": 20 }, "SAN": { "atual": 16, "maximo": 16 }, "PE": { "atual": 3, "maximo": 5 } },
  "pericias": { "INVESTIGACAO": "TREINADO", "PERCEPCAO": "TREINADO" }
}
```

Exemplo de um item da listagem: `{ "id": 7, "sistema": "ORDEM_PARANORMAL", "jogador": "Ana", "personagem": "Jack Moreau", "idade": 38, "retratoVersao": null, "recursos": [ { "codigo": "PV", "atual": 18, "maximo": 20 }, … ] }`.

**Validação de Ordem (400 por campo):**
- atributos de 0 a 5, NEX de 5 a 99 e recursos ≥ 0;
- perícia que não existe no catálogo;
- `VETERANO` com NEX < 35 ("Veterano exige NEX 35% ou mais") e `EXPERT` com NEX < 70.

### 5.2 Rolagem: `POST /fichas/{id}/testes`

Corpo: `{ "alvo": "PERICIA" | "ATRIBUTO" | "SALVAGUARDA", "codigo": "INVESTIGACAO", "modo": "NORMAL" | "VANTAGEM" | "DESVANTAGEM" }`.
- `modo` é opcional e vale `NORMAL` por padrão. Só faz sentido no D&D: em Ordem, `VANTAGEM` ou `DESVANTAGEM` → 400.
- `SALVAGUARDA` só existe no D&D (em Ordem → 400).
- Código de outro sistema → 400.

Resposta:

```json
{ "alvo": "PERICIA", "codigo": "INVESTIGACAO", "nome": "Investigação",
  "dados": [14, 7, 19], "mantido": 19, "bonus": 5, "total": 24 }
```

- `dados` traz **todas** as rolagens, na ordem. O front mostra cada dado e destaca o `mantido`. No D&D, ele marca "20 natural" ou "1 natural" só como informação: não há sucesso automático em testes de perícia.
- **O servidor rola**, com o `Dado` injetável (ADR 0011). Os testes usam um `Dado` com uma sequência fixa.
- Cada sistema é uma classe de regras (`RegrasOrdem`, `RegrasDnd`) com funções puras: quantos dados, qual se mantém e qual é o bônus. Elas são testadas nas fronteiras sem Spring.

**Sai:** `POST /fichas/{id}/testes-de-pericia` (regra d100) e a `RegraTestePericia`. A mudança de contrato é de propósito, e os testes do d100 saem junto, listados no PR.

## 6. Regras

### 6.1 Ordem Paranormal (livro básico; conferido pelo Arthur em 2026-11-04)

- **Atributos:** AGI, FOR, INT, PRE e VIG, de 0 a 5.
- **Treinamento:** destreinado +0, treinado +5, veterano +10 (exige NEX ≥ 35%) e expert +15 (exige NEX ≥ 70%).
- **Teste:** rola **tantos d20 quanto o valor do atributo** e mantém o **maior**. **Atributo 0:** rola **2d20** e mantém o **menor**.
  - **Total da perícia:** dado mantido + bônus do treinamento.
  - **Teste de atributo:** os mesmos dados, sem bônus.
- **NEX:** de 5% a 99%. **Defesa (exibida):** 10 + AGI.
- **PV, SAN e PE máximos:** digitados pelo jogador; o app não calcula pela classe.
- **As 28 perícias e seus atributos:**
  - **AGI:** Acrobacia, Crime, Furtividade, Iniciativa, Pilotagem, Pontaria, Reflexos;
  - **FOR:** Atletismo, Luta;
  - **INT:** Atualidades, Ciências, Investigação, Medicina, Ocultismo, Profissão, Sobrevivência, Tática, Tecnologia;
  - **PRE:** Adestramento, Artes, Diplomacia, Enganação, Intimidação, Intuição, Percepção, Religião, Vontade;
  - **VIG:** Fortitude.

### 6.2 D&D 5e e 2024 (SRD 5.1 e 5.2, CC-BY-4.0) — 2.1e

- **Modificador:** ⌊(valor − 10) / 2⌋.
- **Bônus de proficiência:** 2 + ⌊(nível − 1) / 4⌋, ou seja, +2 nos níveis 1–4 até +6 nos níveis 17–20.
- **Perícia:** d20 + modificador do atributo + proficiência (`PROFICIENTE`) ou 2 × proficiência (`ESPECIALISTA`).
- **Salvaguarda:** d20 + modificador + proficiência, se proficiente. **Teste de atributo:** d20 + modificador.
- **Vantagem/desvantagem:** 2d20, mantendo o maior ou o menor.
- **20/1 natural:** só informativo.
- **As 18 perícias** são iguais nas duas edições, e a lista com os atributos entra no plano do 2.1e, conferida no SRD 5.2.1.
- **Diferenças de 2024 na ficha:** só o rótulo "Espécie" (em 2014, "Raça"). Os outros campos do núcleo são os mesmos.

### 6.3 Licenças e aviso

- **README:** ganha a atribuição do SRD (CC-BY-4.0), com o texto copiado do preâmbulo oficial do SRD 5.1 e do SRD 5.2.1 no 2.1e.
- **Aviso no README:** "app não oficial; Ordem Paranormal é de seus autores e da Jambô Editora; o app usa só nomes e mecânicas, sem texto dos livros".
- **Descrições das perícias (dicas "ⓘ"):** escritas com palavras próprias e curtas.

## 7. Frontend

- **Modelo:** `Ficha` vira uma união discriminada (`FichaOrdem | FichaDnd`) pelo campo `sistema`. Os catálogos (atributos, perícias, recursos e rótulos) ficam em `fichas/sistemas/ordem/` e, depois, em `fichas/sistemas/dnd/`.
- **Criar:** `/fichas/nova` começa pela **escolha do sistema**, com 3 cartões: Ordem Paranormal, D&D 5e (2014) e D&D 2024. No 2.1d, os de D&D aparecem como "em breve". Depois vem o formulário do sistema; a lógica do 2.1 (envio duplo, foco no erro, erro no campo) continua.
- **Painel:** o cartão ganha um selo do sistema e mostra os recursos do resumo, sendo barras de PV/SAN/PE em Ordem. As regras e testes da spec do redesign §3.2 continuam:
  - "Feridos" = PV atual ≤ metade do máximo;
  - "Sanidade baixa" = SAN atual ≤ metade do máximo, e só fichas que têm SAN entram.
- **Ficha de Ordem:**
  - o cabeçalho com o retrato fica igual;
  - os anéis de PV, SAN e PE usam a edição rápida, agora com o `PATCH`;
  - um bloco com classe, NEX, origem e Defesa;
  - os 5 atributos, cada um com "Rolar" (teste de atributo);
  - as 28 perícias, com o selo do grau (T, V ou E), o bônus e "Rolar".
- **Resultado:** o cartão mostra cada dado rolado, o mantido destacado, o bônus e o total, por exemplo "Investigação: 19 + 5 = **24**".
- **Ficha de D&D (2.1e):** PV com temporário na edição rápida; CA, nível, proficiência e inspiração; os 6 atributos com modificador; as salvaguardas; as 18 perícias com marca de proficiência ou especialização; e um seletor **Normal · Vantagem · Desvantagem** antes de rolar.

## 8. Testes

**Backend:**
- migração V4 com fichas d100 (§4);
- regras em testes unitários parametrizados: Ordem com atributo 0, 1 e 5, os três graus e as restrições de NEX; D&D com modificadores nas bordas (1, 10, 11, 30), proficiência por nível (1, 4, 5, 17, 20), especialização e vantagem/desvantagem;
- API por integração (MockMvc + Testcontainers): `POST`/`PUT`/`GET` de cada sistema, discriminador inválido (400), troca de sistema (409), validações, `PATCH` de recursos (código de outro sistema → 400) e rolagem com `Dado` fixo (dados, mantido, bônus e total);
- o retrato e a listagem sem N+1 continuam valendo.

**Frontend:**
- catálogos (28 perícias de Ordem, com os atributos certos);
- a tela de escolha do sistema;
- o formulário de Ordem (validação de NEX para veterano/expert);
- a ficha de Ordem (28 botões de rolar, selo do grau, edição rápida com `PATCH`);
- o resultado com vários dados;
- o painel com o selo e os recursos.

**Regra do processo:** os testes do d100 (regra, serviço, endpoint e telas) **saem de propósito**, e cada um é listado no PR com o motivo. Os testes de comportamento que continuam (CRUD, retrato, edição rápida, painel) são adaptados ao modelo novo e não apagados.

## 9. Riscos

| Risco | Mitigação |
|---|---|
| Regras de Ordem conferidas só pelo Arthur (a pesquisa não achou o livro aberto) | As regras ficam numa classe com testes de fronteira, fáceis de corrigir. O Arthur confere na mesa de jogo. |
| Conversão perde dados do d100 (atributos e perícias) | Decisão consciente do Arthur. O teste de migração prova que nome, jogador, idade, retrato, PV e SAN ficam. |
| Contrato muda muito (listagem vira resumo, rotas novas) | Front e back mudam no mesmo PR, como no 1.6. Não há cliente externo. |
| Uso de nomes de Ordem Paranormal num app público | Só nomes e mecânicas, descrições próprias, aviso de "não oficial" no README. |
| Escopo grande | Dividido em 2.1d (Ordem) e 2.1e (D&D), cada um com PR próprio. |
