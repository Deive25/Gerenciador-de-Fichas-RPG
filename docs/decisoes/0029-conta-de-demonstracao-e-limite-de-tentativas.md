# 0029. Conta de demonstração e limite de tentativas

Status: aceita · Incremento 2.6 · 2026-11-17

## Contexto
Com o link público, um recrutador precisa entrar e ver a mesa funcionando sem criar uma conta de verdade, e a API passa a estar exposta à internet. O [ADR 0022](0022-autenticacao-propria-com-jwt-e-refresh-rotacionado.md) deixou dois riscos para este incremento: **não há limite de tentativas de login** e **os refresh vencidos nunca são apagados**. A demonstração agrava os dois: cada visita cria uma conta, e quem a cria é um endpoint público.

## Decisão
**Conta de demonstração**
- **`POST /auth/demo`**, que só existe com o perfil `demo` (sem ele, 404). Sem corpo, cria um "Visitante NNNN" com `demo = true` (migração V14), sem senha e sem Google, e a campanha "Mesa de demonstração", com uma ficha de Ordem Paranormal e uma de D&D 2024 já preenchidas. A resposta são os tokens, como no login, mais o id da campanha.
- **Convite para a segunda aba:** com `{ "codigoConvite": "..." }`, a API cria um jogador de demonstração, já membro da mesa, com uma ficha própria. O código só vale para campanha cujo mestre seja demo; de campanha real, 404. Uma conta demo também não entra em campanha real. É isso que mostra a rolagem ao vivo: a jogadora rola numa aba e o painel do mestre, na outra, atualiza sozinho.
- **O `DemoService` grava as fichas pelo `FichaRepository`, passando pelos `SistemasDeFicha`.** É uma **exceção consciente** à regra do [ADR 0024](0024-permissoes-de-campanha-num-ponto-central.md) (a regra de acesso fica num ponto só). Aqui não há pedido de uma pessoa sobre a ficha de outra: o serviço cria os dados do próprio visitante que acabou de nascer, então não há permissão a decidir. Passar pelos `SistemasDeFicha` faz as fichas de exemplo seguirem as mesmas regras de criação de qualquer ficha.
- **No front, a sessão demo fica no `sessionStorage` (por aba)**, e não no `localStorage` do [ADR 0023](0023-tokens-no-navegador.md). Com o `localStorage`, a segunda conta trocaria a primeira na renovação, e o mestre e o jogador não conviveriam no mesmo navegador. A demo **nunca apaga o `localStorage`**: sair dela apaga só o refresh da aba, e a conta normal das outras abas continua.

**Limpeza** (fecha o segundo risco do ADR 0022)
- Apaga as contas demo com mais de 24 h, com tudo o que é delas (fichas, retratos, listas, campanhas, membros, rolagens, refresh), e os refresh vencidos. Uma conta **real** que tenha entrado numa mesa demo só perde o vínculo da ficha com a campanha.
- Roda no `ApplicationReadyEvent` (cada vez que a API acorda) e a cada 6 h enquanto está acordada. Um agendamento diário poderia nunca rodar, porque a API dorme. Os refresh já usados, mas ainda válidos, ficam até vencer, porque a detecção de reuso depende deles.

**Limite de tentativas** (fecha o primeiro risco do ADR 0022)
- Um filtro com **janela fixa em memória, por IP e por rota**. `POST /auth/login`, `/auth/registro` e `/auth/google`: 10 por minuto. `POST /auth/demo`: 5 a cada 10 min. Acima disso, **429** com `Retry-After` e a mensagem "Muitas tentativas. Tente de novo em alguns minutos." no formato de erro do projeto (ADR 0007). O `/auth/refresh` fica de fora, porque a rotação já o protege.
- **O IP vem do `getRemoteAddr()`, com `server.forward-headers-strategy=native`.** Atrás do proxy do Render, o Tomcat só confia no `X-Forwarded-For` de um proxy interno. Um cliente que mande o próprio cabeçalho não escolhe o IP que o limite enxerga.

## Alternativas consideradas
- **Pedir cadastro de verdade ao recrutador:** é o jeito mais simples, mas ele desiste antes de ver a mesa. O objetivo era vê-la em 2 minutos.
- **Uma conta de demonstração compartilhada:** um visitante estragaria os dados do outro.
- **Redis ou Bucket4j para o limite:** só faria sentido com mais de uma instância. Com uma, a memória basta; fica registrado como o caminho se o projeto escalar.
- **`@Scheduled` diário para a limpeza:** pode nunca rodar numa API que dorme; por isso o gatilho no arranque.
- **Confiar no `X-Forwarded-For` de qualquer origem:** deixaria o atacante escolher o IP e burlar o limite.

## Consequências
- Quem abre o link entra na demonstração em um clique, e um convite aberto numa segunda aba mostra a mesa ao vivo.
- O banco gratuito (1 GB) não enche de contas demo: o limite do `/auth/demo` e a limpeza de 24 h seguram o crescimento.
- **Pior:** o limite vive na memória e **some a cada reinício**. Como a API dorme e acorda, ele zera a cada acordada, e não vale entre duas instâncias.
- **Pior:** atrás de uma rede compartilhada (uma empresa, uma universidade), muitas pessoas têm o mesmo IP e dividem as 5 demonstrações a cada 10 min.
- **Pior:** **duplicar uma aba copia o `sessionStorage`**, e as duas abas passam a ter o mesmo refresh. Quando uma renova, o refresh da outra fica velho, e a detecção de reuso (ADR 0022) derruba as duas. É o preço do `sessionStorage` por aba, e só atinge a demo.
- **Pior:** a conta demo some em 24 h, de propósito; quem quiser guardar uma ficha deve criar uma conta de verdade.
- **Pior:** o `DemoService` é um segundo caminho de criação de fichas, fora do `AcessoAFicha`. Se a regra de acesso mudar, é preciso lembrar que ele não passa por ela.
