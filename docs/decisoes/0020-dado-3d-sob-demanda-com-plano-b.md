# 0020. Dado 3D sob demanda, atrás de uma interface e com plano B

Status: aceita · Incremento 2.1f · 2026-11-11

## Contexto
O Arthur pediu que a rolagem fosse animada: um d20 em 3D por dado rolado. São de 1 a 5 em Ordem Paranormal, ou 2 com atributo 0, e 1 ou 2 em D&D (vantagem ou desvantagem). Quem sorteia continua sendo o servidor ([ADR 0011](0011-teste-de-pericia-e-aleatoriedade-injetavel.md)): a animação só pode mostrar os valores que a API devolveu em `dados`. Um motor 3D com física pesa algumas centenas de kB, e o painel precisa abrir rápido no celular. O jsdom, onde rodam os testes do front, não tem WebGL.

## Decisão
- **Biblioteca `open-dice-dnd` 1.13.0** (MIT). Ela usa `three` 0.130.1 e `cannon-es` 0.20.0, também MIT, e as três versões ficam fixas. A biblioteca aceita o valor de cada dado (`rolled`): ela calcula a física antes e pinta o valor pedido na face que vai cair para cima. O teste rápido do 2.1f confirmou que 5 d20 param nos valores forçados, sem divergência, no computador e no celular emulado.
- **Carregamento sob demanda:** a biblioteca só é importada na primeira rolagem (`import()` dinâmico) e fica num pedaço próprio, o `lancador-3d`, de 849,05 kB, ou 197,94 kB transferidos. O pacote inicial (317,80 kB) não ganhou o `three`.
- **Uma interface separa a tela da cena 3D:**
  - o `LancadorDeDados` tem dois métodos: `lancar(palco, dados, indiceMantido)`, que resolve quando os dados param, e `encerrar()`;
  - só o `lancador-3d.ts` importa a biblioteca;
  - o serviço `DadoAnimado` decide entre 3D e plano B, e os testes trocam o carregador por um falso via `InjectionToken`;
  - as cores saem de uma função pura, testada sem WebGL.
- **Plano B automático:** o resultado aparece direto, sem animação, quando o sistema pede movimento reduzido, quando não há WebGL ou quando o `import()` falha. A falha vai só para o console, porque a rolagem em si funcionou.
- **A cena é criada a cada rolagem e destruída no fim**, para não sobrar contexto WebGL aberto quando o jogador sai da ficha.

## Alternativas consideradas
- **`@3d-dice/dice-box`:** cerca de 11 MB, e sem resultado forçado documentado.
- **`threejs-dice` e `dice-box-threejs`:** estão sem manutenção.
- **`react-ttrpg-dice`:** só funciona com React.
- **Animação 2D** (d20 em SVG trocando de número com CSS): sem dependência nova, mas sem o dado físico que o Arthur pediu. É o plano B da spec, caso a biblioteca não passasse no teste rápido.
- **Importar a biblioteca no pacote da ficha:** é mais simples, mas faz quem só lê a ficha baixar o motor 3D.

## Consequências
- O servidor continua sendo a fonte da verdade. A animação não muda o resultado e pode ser pulada com um toque ou com Esc.
- A regra de qual dado fica e de quantos dados rolar continua no backend. O front só desenha `dados` e destaca o mantido, que é o primeiro dado com o valor de `mantido`, a mesma regra do cartão.
- **Pior:** o `three` fica preso no 0.130, de 2021, enquanto a biblioteca exigir essa versão. Atualizá-lo depende dela.
- **Pior:** a física anda por quadro de animação, e não por tempo. Num aparelho lento, a rolagem demora mais (cerca de 2,2 s a 60 fps). Por isso há um limite de 5 s, depois do qual o resultado aparece mesmo assim.
- **Pior:** a cena 3D real não roda nos testes automáticos (o jsdom não tem WebGL). Ela é conferida à mão no navegador, a 375 e a 1280 px.
