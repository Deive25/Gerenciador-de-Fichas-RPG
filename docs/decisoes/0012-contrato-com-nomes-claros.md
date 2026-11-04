# 0012. Contrato da API com nomes claros

Status: aceita · Incremento 1.6 · 2026-11-03

## Contexto
O JSON herdado tinha 41 chaves com nomes que não dizem nada (`nameJ`, `lifeA`, `str`, `atribute1`..`atribute23`) e a rota `/sheetPlayer`. Desde o 1.4b as perícias são um enum no banco, mas a API ainda as escondia atrás de `atributeN`. O Angular é o único consumidor e está no mesmo repositório.

## Decisão
- Rota `/fichas` (e `/fichas/{id}/testes-de-pericia`); `Location: /fichas/{id}`. Nenhuma rota antiga é mantida.
- 18 campos escalares planos em português (`jogador`, `personagem`, `genero`, `idade`, `vidaAtual`, `vidaMaxima`, `sanidadeAtual`, `sanidadeMaxima`, `forca`, `destreza`, `constituicao`, `inteligencia`, `carisma`, `sabedoria`, `tamanho`, `sorte`, `poder` e o `id`) e um objeto `pericias` chaveado pelo código do enum, só com as preenchidas.
- Entrada: código fora do enum em `pericias` dá 400; valor `null` conta como perícia ausente (o Angular manda as 23 com `null`); perícia omitida no PUT é removida (o PUT já era substituição completa, ADR 0008). As demais regras e os códigos HTTP não mudam.
- Entidade, colunas e migrações não mudam: o `SheetPlayerMapper` traduz. O `campoLegado` do enum `Pericia` fica só para a V2 e para os testes.
- O 400 de validação passa a dizer "Há campos inválidos na requisição", sem citar "ficha".
- Backend e frontend mudam no mesmo incremento.

## Alternativas consideradas
- Versionar (`/v2`) e manter a v1: duas APIs para manter, sendo que o único cliente muda junto; versionar paga quando há clientes que não controlo.
- Manter os nomes antigos: o custo de entender o JSON continuaria e o enum ficaria escondido.
- Objetos aninhados (`vida: {atual, maxima}`): mais fiel ao domínio, mas obriga o formulário e a entidade a mudar de forma; ficou fora para manter o passo pequeno.
- Nomes em inglês: é o padrão da indústria, mas o domínio (perícia, sanidade) e a tela estão em português, e traduzir o jogo cria termos artificiais.

## Consequências
- O JSON agora se explica sozinho e o Angular usa o mesmo código do enum que o teste de perícia.
- Pior: é uma quebra de contrato; qualquer cliente fora do repositório para de funcionar sem aviso.
- Pior: o mapper agora traduz nomes (`nameJ` na entidade, `jogador` no JSON), uma camada de indireção até a entidade e as colunas serem renomeadas.
- Pior: o frontend continua em Angular 13, que o `ng build` só compila em Node 16; o upgrade ficou para uma decisão própria.
