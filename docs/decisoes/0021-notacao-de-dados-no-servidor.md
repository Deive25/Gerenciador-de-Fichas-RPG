# 0021. Uma notação de dados no servidor, para o dano e a rolagem livre

Status: aceita · Incremento 2.1i · 2026-11-11

## Contexto
O 2.1i traz duas rolagens novas:
- **O ataque:** o d20 do ataque mais o dano, que o jogador escreve na notação de mesa (`1d8+3`, `2d6+1d4+2`). No crítico, os dados do dano multiplicam.
- **A rolagem livre:** uma aba onde a pessoa escolhe quantos e quais dados rolar (d4 a d100) e um modificador, por botões ou digitando.

As duas precisam ler a mesma notação. O servidor já é quem sorteia todos os dados ([ADR 0011](0011-teste-de-pericia-e-aleatoriedade-injetavel.md)), e o dado 3D só mostra o que ele devolveu ([ADR 0020](0020-dado-3d-sob-demanda-com-plano-b.md)).

## Decisão
- **Notação e sorteio no servidor:** uma função pura, `NotacaoDeDados`, lê a notação e rola com a interface `Dado`. Ela serve ao dano dos ataques e à rolagem livre (`POST /fichas/{id}/rolagens`).
- **A gramática** é a da spec (§6.3), com três ajustes:
  - **até 8 termos**, e não 5, porque os botões da aba Dados geram 7 tipos de dado mais o modificador;
  - **até 20 dados somados**, para a cena 3D não pesar;
  - **dados não podem ser subtraídos** (`1d8-1d4` dá 400); só as constantes subtraem.
- **No crítico, os dados multiplicam e as constantes não:** ×2 em D&D, e o `multiplicador` da arma em Ordem. Um total negativo vira 0.
- **A resposta traz `lados` e `valor` de cada dado**, para o 3D desenhar o tipo certo.
  - O d10 e o d100 seguem a mesa: o 10 cai no "0", e o 100 cai no "00" + "0".
  - Com mais de 20 dados na cena, o resultado aparece direto (plano B).
- **O front repete a gramática** numa função pura (`lerNotacao`), só para mostrar o erro no campo e desabilitar "Rolar" sem ir à rede. O servidor continua sendo a autoridade.
- **A rolagem livre fica sob a ficha** (`/fichas/{id}/rolagens`), e não numa rota solta. A aba está na ficha, e nas rolagens ao vivo (2.4) cada rolagem vai pertencer a um personagem.

## Alternativas consideradas
- **Rolar a rolagem livre no navegador:** seria mais simples, mas abriria uma exceção à regra "o servidor rola", que importa quando a mesa estiver ao vivo.
- **Pedido estruturado** (`{ "d6": 2, "modificador": 3 }`) **em vez de texto:** é fácil de validar, mas a notação já existe por causa do dano. Ter duas formas de dizer a mesma coisa seria pior. Os botões só escrevem a notação.
- **Uma regex só:** fica curta, mas dá mensagens de erro ruins. O leitor por termos diz qual é o problema ("no máximo 20 dados", "use d4, d6, d8, d10, d12, d20 ou d100").

## Consequências
- Uma regra, com testes de válidos e inválidos dos dois lados, serve ao dano, ao crítico e à rolagem livre.
- **Pior:** a gramática existe em Java e em TypeScript. Se um lado mudar e o outro não, o front pode aceitar o que o servidor recusa. Nesse caso o 400 aparece no campo, e os testes repetem os mesmos casos nos dois lados.
- **Pior:** com o limite de 20 dados, uma rolagem grande de verdade (40d6) não cabe. É raro numa mesa, e o limite pode subir se a cena 3D aguentar.
