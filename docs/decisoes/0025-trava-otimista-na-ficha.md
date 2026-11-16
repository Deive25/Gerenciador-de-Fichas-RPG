# 0025. Trava otimista na ficha

Status: aceita · Incremento 2.3 · 2026-11-16

## Contexto
Até o 2.2, cada ficha tinha um único editor, o dono, e a última gravação vencia. Com as campanhas (2.3), o mestre também edita a ficha do jogador. O `PUT /fichas/{id}` é uma substituição completa (ADR 0008): se o jogador abre o formulário, o mestre muda o PV e o jogador salva, o PV do mestre some sem ninguém perceber.

## Decisão
- **`@Version` na ficha** (coluna `versao` na tabela `ficha`, a raiz da herança JOINED). A versão vai na resposta da ficha completa.
- **O `PUT` precisa enviar a `versao` que leu.** Sem ela, 400 em `errors.versao`; diferente da do banco, **409 "Esta ficha mudou enquanto você editava. Recarregue para ver a versão nova."**. A corrida entre dois pedidos simultâneos (`ObjectOptimisticLockingFailureException`) vira o mesmo 409, num tratador só.
- **As rotas de um clique** (edição rápida de PV, moedas, espaços de magia e a troca de campanha) **não enviam versão**, porque gravam só o próprio valor, mas **sobem** a versão. Assim, um formulário aberto em outra aba não apaga a mudança. As listas (itens, magias, ataques, habilidades) e o retrato são linhas próprias e não mexem na versão.
- **Sem nova tentativa automática.** O conflito nessas rotas só acontece quando dois pedidos se sobrepõem no servidor, por milissegundos, e não com cliques em sequência.
- **No front:** o formulário envia a versão, passa a usar a da resposta (dois salvamentos seguidos não dão 409) e, no 409, mostra o aviso com "Recarregar", que traz a versão nova e descarta o que foi digitado.

## Alternativas consideradas
- **A última gravação vence:** é a mais simples, mas o mestre perderia mudanças sem saber, que é o pior tipo de erro numa mesa.
- **Trava pessimista** (marcar a ficha como "em edição"): ninguém perderia nada, mas um formulário esquecido aberto bloquearia a ficha para todos. Exigiria prazo, desbloqueio e uma tela de "quem está editando".
- **Mesclar campo a campo:** salvaria as duas mudanças quando não se cruzam, mas exigiria guardar a versão anterior de cada campo e decidir os conflitos. É complexo demais para um conflito raro.
- **Exigir a versão também nas rotas de um clique:** protegeria contra a corrida, mas faria o PV falhar no meio do jogo quando o mestre e o jogador clicam juntos.

## Consequências
- A mudança do mestre nunca some em silêncio: quem salva por cima de uma versão velha recebe o aviso.
- **Pior:** a versão sobe também na edição rápida, então quem está com o formulário aberto recebe o 409 mesmo quando a outra pessoa mudou um campo diferente. Numa mesa isso é raro, e o "Recarregar" resolve.
- **Pior:** "Recarregar" descarta o que foi digitado. Guardar o rascunho para reaplicar fica como melhoria, se o conflito aparecer na prática.
- O contrato do `PUT` mudou: os testes de `PUT` existentes passaram a enviar `versao`, e o `ContratoJsonTest` ganhou a chave.
