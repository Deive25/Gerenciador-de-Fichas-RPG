# J1 fase 2: abas da ficha (descrição, habilidades, inventário, ataques, magias e rituais) — design

- **Status:** aprovado em conversa pelo Arthur em 2026-11-09 ("Aprovar tudo"). Aguarda a revisão desta spec escrita.
- **Base:** `main` em `bd13753` (2.1e mesclado). Backend com 210 testes, frontend com 260, ADRs 0001–0017.
- **Relação com outras specs:**
  - esta spec amplia o escopo da spec dos sistemas (`2026-11-04-j1-sistemas-design.md`), cujo §1 deixava inventário, magias, rituais, ataques e habilidades fora do núcleo jogável;
  - o dado animado (2.1f, spec do redesign §5 revista) **vem depois** destes incrementos, por decisão do Arthur.

## 1. Objetivo

O jogador quer guardar na ficha tudo o que usa na mesa, além do núcleo jogável.

**Escolhas do Arthur (2026-11-09):**
- **Ordem:** as abas vêm antes do dado animado.
- **Conteúdo:** **o jogador digita**. O app não traz magias, itens nem rituais prontos; só os campos de cada sistema.
- **Abas:** equipamento/inventário, magias e rituais, ataques, habilidades e descrição.
- **Profundidade:** **registro mais contas simples**: somas de carga, CD, DT e custo, e a rolagem de ataque e dano.
- **Abordagem A:** tabelas tipadas por lista (comuns onde os sistemas coincidem, separadas onde diferem), cada lista editada pela própria rota.

**Sucesso:** numa ficha de cada sistema, adicionar, editar e remover itens em cada aba, ver os totais certos e rolar um ataque com dano e crítico, a 375 px e a 1280 px.

**Fora do escopo:**
- catálogos prontos (magias do SRD, itens e rituais dos livros);
- gastar PE ou espaços de magia automaticamente ao conjurar;
- penalidade automática por carga acima do limite (o app só mostra o aviso);
- condições, efeitos e duração em rodadas;
- reordenar itens arrastando (a ordem é a de criação).

## 2. Incrementos

Mesmo fluxo da fase 2: TDD → verificador separado → documentação → PR com o CI verde, mesclado como *merge commit*.

| Inc. | Entrega | Pronto quando |
|---|---|---|
| **2.1g** | As **abas** na tela da ficha, mais **Descrição** (aparência, história e anotações) e **Habilidades**, nos dois sistemas. Cria o padrão de lista (rotas, componente de lista, formulário inline) que os próximos reaproveitam. | Testes de integração e de front verdes; as abas, a descrição e as habilidades funcionam a 375 px e a 1280 px, e recarregar mantém a aba aberta. |
| **2.1h** | **Inventário**: Ordem com espaços e categoria; D&D com peso, equipado e moedas. Mostra a carga usada contra o limite. | Testes das contas de carga nas bordas; fluxo completo nas duas larguras. |
| **2.1i** | **Ataques**, com a rolagem de ataque e dano pela notação e o crítico de cada sistema. | Testes da notação e do crítico; rolagem conferida no navegador. |
| **2.1j** | **Magias** (D&D, com espaços por nível, CD e ataque mágico) e **Rituais** (Ordem, com custo em PE e DT). | Testes das contas; fluxo completo nas duas larguras. |

**Novo ADR (no 2.1g):** **0018**, "listas da ficha em recursos próprios". As listas ficam fora do `PUT` da ficha, então o formulário não as apaga. Isso refina o ADR 0008.

## 3. Modelo de dados

Todas as listas têm `id bigint identity` e uma FK para a ficha (ou para a tabela do sistema) com `on delete cascade`. Os textos curtos têm até 255 caracteres e as descrições até 4000. Ao ler, a ordem é a de criação (`id`).

### 3.1 2.1g — comum aos dois sistemas
- **`ficha`** ganha `aparencia`, `historia` e `anotacoes` (`varchar(4000)`, opcionais).
- **`ficha_habilidade`**: `ficha_id` → `ficha`; `nome` obrigatório; `tipo` obrigatório; `descricao` opcional.
  - Tipos em **Ordem**: `CLASSE`, `TRILHA`, `ORIGEM`, `PARANORMAL`, `OUTRA`.
  - Tipos em **D&D**: `CLASSE`, `SUBCLASSE`, `ESPECIE`, `ANTECEDENTE`, `TALENTO`, `OUTRA`.
  - O banco aceita a união dos tipos (check). Quem valida o tipo do sistema é o serviço.

### 3.2 2.1h — inventário
- **`ficha_ordem_item`**: `ficha_id` → `ficha_ordem`; `nome`; `categoria` de 0 a 4 (mostrada como 0, I, II, III, IV); `espacos` inteiro ≥ 0; `quantidade` ≥ 1; `descricao`.
- **`ficha_dnd_item`**: `ficha_id` → `ficha_dnd`; `nome`; `quantidade` ≥ 1; `peso` `numeric(7,2)` ≥ 0, em kg; `equipado` boolean (padrão falso); `descricao`.
- **`ficha_dnd`** ganha as moedas `pc`, `pp`, `pe`, `po` e `pl` (inteiros ≥ 0, padrão 0).

### 3.3 2.1i — ataques
- **`ficha_ordem_ataque`**:
  - `ficha_id`, `nome`;
  - `pericia` (`LUTA` ou `PONTARIA`);
  - `bonus` inteiro (pode ser negativo, padrão 0);
  - `dano` (notação, §6.3);
  - `tipo_dano` e `alcance` (texto, opcionais);
  - `margem` de 2 a 20 (padrão 20);
  - `multiplicador` de 2 a 5 (padrão 2).
- **`ficha_dnd_ataque`**:
  - `ficha_id`, `nome`;
  - `atributo` (código de `AtributoDnd`);
  - `proficiente` boolean;
  - `bonus` inteiro (padrão 0);
  - `dano`, `tipo_dano` e `alcance`.

### 3.4 2.1j — magias e rituais
- **`ficha_dnd_magia`**:
  - `ficha_id`, `nome`;
  - `nivel` de 0 a 9 (0 = truque);
  - `escola`, `tempo`, `alcance`, `componentes` e `duracao` (texto, opcionais);
  - `concentracao`, `ritual` e `preparada` (boolean);
  - `descricao`.
- **`ficha_dnd_espaco`**: `ficha_id`; `nivel` de 1 a 9; `total` ≥ 0; `usados` ≥ 0 e ≤ `total`. A PK é `(ficha_id, nivel)`.
- **`ficha_dnd`** ganha `atributo_conjuracao` (`INT`, `SAB`, `CAR` ou nulo).
- **`ficha_ordem_ritual`**:
  - `ficha_id`, `nome`;
  - `circulo` de 1 a 4;
  - `elemento` (`SANGUE`, `MORTE`, `CONHECIMENTO`, `ENERGIA` ou `MEDO`);
  - `execucao`, `alcance`, `alvo`, `duracao` e `resistencia` (texto, opcionais);
  - `descricao`.

## 4. API

Continuam valendo: JSON estrito, ProblemDetail em português e o servidor como quem rola.

- **Leitura:** `GET /fichas/{id}` passa a trazer as listas do sistema da ficha, mais `descricao` (`{aparencia, historia, anotacoes}`). A listagem `GET /fichas` não muda e continua em 1 consulta.
- **Escrita por lista** (`{lista}` = `habilidades`, `itens`, `ataques`, `magias`, `rituais`):
  - `POST /fichas/{id}/{lista}` → 201 com o item criado e `Location`;
  - `PUT /fichas/{id}/{lista}/{itemId}` → 200 com o item (substitui o item inteiro);
  - `DELETE /fichas/{id}/{lista}/{itemId}` → 204.
- **Outras escritas:**
  - `PUT /fichas/{id}/descricao` → 200;
  - `PATCH /fichas/{id}/moedas` (D&D) → 200;
  - `PATCH /fichas/{id}/espacos` (D&D; `{ "1": {total, usados}, … }`) → 200.
- **Rolagem de ataque:** `POST /fichas/{id}/ataques/{ataqueId}/rolagem`, com o corpo `{ "modo": … }` opcional e só para D&D. A resposta é `{ ataque: {dados, mantido, bonus, total}, dano: {dados, bonus, total}, critico }`.
- **Erros:**
  - item que não pertence à ficha → **404**;
  - lista que não existe no sistema da ficha (por exemplo, `rituais` em D&D) → **400** com `errors.lista` ("não existe em D&D");
  - campo inválido → **400** com `errors` por campo (inclusive notação de dano inválida em `errors.dano`).
- **O `PUT /fichas/{id}` (formulário) não mexe nas listas nem na descrição** (ADR 0018).

## 5. Frontend

- **Topo fixo:** retrato, menu ⋮ e anéis de recursos continuam no topo da ficha, sempre visíveis.
- **Abas (`mat-tab-group`) logo abaixo:**
  - Ordem: **Ficha**, **Ataques**, **Inventário**, **Rituais**, **Habilidades**, **Descrição**;
  - D&D: **Ficha**, **Ataques**, **Inventário**, **Magias**, **Habilidades**, **Descrição**;
  - "Ficha" é o corpo que existe hoje (atributos, perícias e rolagem).
- **Celular:** as abas rolam na horizontal, e a aba aberta fica na URL (`?aba=inventario`).
- **Listas:**
  - um componente genérico de lista, com "Adicionar", editar inline e remover com confirmação;
  - cada aba fornece os campos do seu formulário e o cartão de cada item;
  - o 2.1g cria o padrão e os outros incrementos só acrescentam abas.
- **Totais no topo de cada aba:**
  - espaços usados contra o limite (Ordem) e peso total contra a capacidade (D&D), com aviso quando passa;
  - CD e ataque mágico (D&D);
  - custo em PE e DT de cada ritual (Ordem).
- **Ataques:** "Rolar" mostra o cartão de resultado com o ataque (os dados e o mantido) e o dano (os dados e o total), mais o selo "Crítico" quando houver. Em D&D, o seletor Normal · Vantagem · Desvantagem é o mesmo da aba Ficha.
- **Espaços de magia:** a pessoa marca e desmarca o que já usou, e há um "Descanso longo" que zera todos os usados (`PATCH`).
- **Mesmos cuidados do 2.1b:** contraste AA, alvos de 44 px e nada transbordando a 375 px.

## 6. Regras

### 6.1 D&D (SRD 5.1 e 5.2.1, CC-BY-4.0)
- **Capacidade de carga** = FOR × 7,5 kg (as 15 lb do SRD). O peso das moedas é ignorado.
- **Ataque** = d20 + modificador do atributo + proficiência (se proficiente) + `bonus`, com vantagem ou desvantagem.
- **Crítico:** 20 natural no dado mantido do ataque. Os **dados** de dano dobram, e o bônus fixo não.
- **CD de magia** = 8 + proficiência + modificador do atributo de conjuração.
- **Ataque mágico** = proficiência + modificador. Sem atributo de conjuração, os dois aparecem como "—".

### 6.2 Ordem Paranormal (propostas do Opus, aprovadas pelo Arthur em 2026-11-09)
- **Limite de carga:** 5 espaços por ponto de Força; com Força 0, 2 espaços.
- **Custo em PE por círculo:** 1º = 1, 2º = 3, 3º = 6, 4º = 10.
- **DT dos rituais:** 10 + limite de PE + Presença, sendo o limite de PE = ⌊NEX / 5⌋ (NEX 5% → 1).
- **Ataque:** o teste da perícia do ataque (Luta → FOR, Pontaria → AGI), com os d20 do atributo e o grau da perícia, como na rolagem de perícia, mais o `bonus`.
- **Crítico:** se o dado mantido for ≥ `margem`, os **dados** de dano são multiplicados pelo `multiplicador`. O bônus fixo não é multiplicado.
- **Espaços:** inteiros.

### 6.3 Notação de dano (as duas)
- Termos separados por `+` ou `−`. Cada termo é `NdM` (N de 1 a 20; M em 4, 6, 8, 10, 12, 20 ou 100) ou uma constante de 0 a 99. São no máximo 5 termos.
- **Exemplos válidos:** `1d8+3`, `2d6+1d4+2`, `1d10-1`.
- **Inválidos:** `d6`, `0d6`, `1d7`, `21d6`, `abc` e vazio.
- **Total:** a soma dos dados mais as constantes. Se der negativo, o total é 0.
- **No crítico**, o N de cada termo de dados é multiplicado (D&D por 2; Ordem pelo `multiplicador`).

### 6.4 Licenças
Nenhum texto dos livros entra no app: só os nomes dos campos. Os nomes de elementos e círculos de Ordem são mecânica, e o aviso de "não oficial" do README cobre o caso.

## 7. Testes

**Backend (em cada incremento):**
- integração de cada lista:
  - criar (201 + `Location`), editar e remover;
  - 400 por campo;
  - 404 para item de outra ficha;
  - 400 para lista de outro sistema;
  - cascata ao excluir a ficha;
- o `PUT /fichas/{id}` não apaga as listas nem a descrição;
- `ContratoJsonTest` com as chaves novas;
- `ListagemSemNMaisUmTest` continua em 1 consulta;
- regras unitárias:
  - a notação (válidos e inválidos da §6.3);
  - o crítico dos dois sistemas;
  - a carga nas bordas (FOR 0, 1 e 5);
  - a CD e o ataque mágico;
  - o custo e a DT dos rituais (NEX 5, 35 e 99);
  - a rolagem de ataque com `Dado` fixo.

**Frontend:** as abas e a URL `?aba=`; o componente genérico de lista (adicionar, editar, remover e confirmar); cada aba com os seus campos e totais; a rolagem de ataque com crítico.

**Navegador:** em cada incremento, conferência a 375 px e a 1280 px numa ficha de cada sistema, com backup do banco local antes de cada migração.

## 8. Riscos

| Risco | Mitigação |
|---|---|
| As regras de Ordem foram propostas de memória e aprovadas em bloco | Ficam em funções puras com testes de fronteira, fáceis de corrigir. O Arthur confere na mesa. |
| Muitas tabelas e rotas novas | O padrão de lista do 2.1g se repete nos outros incrementos, com um componente genérico no front e o mesmo formato de rota no back. |
| A ficha completa fica pesada para carregar | As listas só são lidas no detalhe, dentro da transação. A listagem não as toca. |
| A notação de dano aceita entradas estranhas | Gramática fechada (§6.3), com testes de inválidos e limite de termos. |
