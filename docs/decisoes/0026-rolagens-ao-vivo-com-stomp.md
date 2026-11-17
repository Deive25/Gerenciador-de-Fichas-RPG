# 0026. Rolagens ao vivo com STOMP

Status: aceita · Incremento 2.4 · 2026-11-17

## Contexto
Até o 2.3, cada pessoa via só as próprias rolagens, e nada era gravado. O objetivo do 2.4 é a mesa ver as rolagens de todos na hora, e quem chega depois ver o histórico. Há três rotas que rolam dados (teste, ataque e rolagem livre), e o navegador precisa receber a rolagem de outra pessoa sem recarregar a tela. Duas regras vêm do 2.3: a ficha do mestre (NPC) fica oculta para os jogadores, e a ficha pode estar sem campanha.

## Decisão
- **As três rotas chamam o `RegistroDeRolagens`**, depois de rolar. Se a ficha está numa campanha, ele grava a linha na tabela `rolagem` (migração V12) e publica um evento de domínio. Ficha sem campanha não grava nem publica. **Regra para o futuro: toda rota que rola dados chama o `RegistroDeRolagens`**, e isso está escrito no comentário do componente.
- **A publicação só acontece depois do commit:** um `@TransactionalEventListener(AFTER_COMMIT)` envia a rolagem a `/topic/campanhas/{id}/rolagens`. Se a transação for desfeita, nada sai. Se o envio falhar, só vai para o log, porque a rolagem já está gravada e aparece no histórico.
- **STOMP sobre WebSocket, em `/ws`**, com o broker simples do Spring, em memória. O front usa o `@stomp/rx-stomp`, que reconecta sozinho.
- **O handshake é público e a autenticação acontece no `CONNECT`:** o front manda `Authorization: Bearer <token>` nos cabeçalhos do STOMP, e um interceptor valida o token com o `JwtDecoder` que a API já usa. O `SUBSCRIBE` só aceita `/topic/campanhas/{id}/rolagens` de uma campanha da qual a pessoa participa. O `SEND` do cliente é recusado: a mesa só recebe.
- **O nome do NPC não é gravado.** Quando a ficha é do mestre da campanha, `personagem` fica nulo, e a resposta traz `doMestre: true` e nenhum id de ficha. Todos veem "Mestre". Se o mestre rola com a ficha de um jogador, a linha mostra "(rolado pelo mestre)".
- **Histórico por cursor:** `GET /campanhas/{id}/rolagens?antes=<id>&limite=50` (1 a 100). O índice `(campanha_id, id desc)` segue essa ordem.
- **Teste de ponta a ponta no CI:** um teste Playwright com duas sessões de navegador (o mestre vê a rolagem da jogadora no painel). O job `e2e` sobe PostgreSQL, API e front do zero.

## Alternativas consideradas
- **SSE (Server-Sent Events):** é mais simples que o WebSocket e passa melhor por proxies, mas só envia do servidor ao navegador, e o `EventSource` do navegador não aceita o cabeçalho `Authorization`. Eu precisaria mandar o token na URL, que vai parar em logs. O STOMP também já estava aprovado na spec da fase 2.
- **Polling:** o mais simples de todos, mas a mesa veria a rolagem com atraso de alguns segundos e a API receberia pedidos o tempo todo, mesmo sem ninguém rolando. Não cumpre "ao vivo".
- **AOP (`@Aspect`) nos três métodos:** os serviços não mudariam, mas o aspecto teria de adivinhar a ficha e o tipo pela assinatura. É mágico de ler, de testar e de explicar. Preferi três chamadas visíveis.
- **O front enviar o resultado** (`POST` com o que o navegador rolou): é o mais simples, mas qualquer pessoa forjaria um 20 natural. A rolagem é sorteada e gravada no servidor.
- **Spring Security messaging, com CSRF no CONNECT:** é o caminho "completo" do Spring para autorizar mensagens, mas a API é sem sessão e sem cookie (o token vai no cabeçalho), então o CSRF não se aplica, e a configuração seria grande para duas regras (token válido no `CONNECT`, membro no `SUBSCRIBE`). Um `ChannelInterceptor` pequeno resolve as duas.

## Consequências
- A mesa vê a rolagem na hora, e o histórico não perde nada. Os contratos das três rotas `POST` não mudaram, e nenhuma asserção dos testes antigos mudou.
- O nome do NPC não existe no banco, então não vaza por nenhuma consulta futura. O custo é não saber depois qual NPC rolou.
- **Pior:** a autenticação acontece só no `CONNECT`. A conexão aberta **continua valendo depois de o token de 15 min vencer**, e **quem é removido da campanha continua recebendo as rolagens até a conexão cair** (ou até sair da tela). O histórico, ao contrário, confere a participação a cada `GET`. No `CONNECT` seguinte (reconexão), o front manda o token atual e renova antes, se for preciso.
- **Pior:** o broker simples vive na memória de uma instância. Com duas instâncias da API, quem está ligado numa não receberia o que foi rolado na outra. Para escalar, troca-se por RabbitMQ ou Redis com relay STOMP, sem mudar o front. Para o portfólio, uma instância basta.
- **Pior:** três chamadas para lembrar. Uma rolagem nova que esquecesse o `RegistroDeRolagens` não apareceria para a mesa. A mitigação é o comentário no componente e a conferência do verificador a cada incremento.
- **Pior:** proxies e o plano gratuito do Render podem cortar um WebSocket parado. O cliente usa heartbeat de 10 s e reconecta; o painel busca de novo a primeira página do histórico para recuperar o que passou durante a queda. Reavaliar no deploy (2.6).
- O teste de ponta a ponta é mais lento e mais sujeito a instabilidade que os unitários. Ele espera pelo texto na tela (e não por tempo fixo), e o CI guarda relatório e rastreamento quando falha.
