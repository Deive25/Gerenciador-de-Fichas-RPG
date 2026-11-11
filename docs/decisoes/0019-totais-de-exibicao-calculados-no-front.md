# 0019. Totais de exibição calculados no front

Status: aceita · Incremento 2.1h · 2026-11-10

## Contexto
A aba Inventário mostra a carga usada contra o limite. Em Ordem Paranormal, são os espaços usados contra 5 × Força (com Força 0, o limite é 2). Em D&D, é o peso total contra Força × 7,5 kg. O total muda a cada item adicionado, editado ou removido. A API guarda os itens e os atributos, e não expõe nenhum total.

## Decisão
- **Contas como funções puras no front:** `limiteDeCarga` e `espacosUsados` em `catalogo-ordem.ts`, e `capacidadeDeCarga` e `pesoTotal` em `regras-dnd.ts`. Todas têm testes de borda (Força 0 em Ordem, 3 unidades de 0,1 kg em D&D).
- **Valores por unidade:** a carga de Ordem soma `espacos × quantidade`, e o peso de D&D soma `peso × quantidade`. O peso é arredondado a 2 casas, para não aparecer ruído de ponto flutuante (0,1 × 3 = 0,3, e não 0,30000000000000004).
- **O servidor guarda só os dados brutos** e não calcula a carga.
- **A regra não trava nada:** o app só avisa quando a carga passa do limite (`>`, e não `>=`). A penalidade por excesso de carga fica fora do escopo da spec.

## Alternativas consideradas
- **Calcular no servidor e devolver no `GET /fichas/{id}`:** haveria uma fonte só da regra, mas cada mudança de item exigiria buscar a ficha de novo, ou repetir a conta no front de qualquer jeito.
- **Calcular nos dois lados, como já acontece com os bônus de D&D:** ali o servidor precisa da regra para rolar os dados. Aqui ele não usaria a conta para nada, e seria código sem uso.

## Consequências
- O total muda na hora, sem requisição a mais.
- **Pior:** outro cliente da API (um app de celular, por exemplo) teria de repetir a conta.
- **Pior:** a regra fica em TypeScript, fora dos testes do backend. Se um dia o servidor precisar dela (para travar um item acima do limite), a conta sobe para lá, e este ADR é revisto.
