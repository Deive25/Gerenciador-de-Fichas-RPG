# Frontend (Angular 22)

Interface do Gerenciador de Fichas RPG. Visão geral, como rodar a API e decisões: veja o [README principal](../README.md).

```sh
npm ci
npm start                    # http://localhost:4200 (a API precisa estar em http://localhost:8080)
npx ng test --watch=false    # testes unitários (Vitest)
npx ng build                 # build de produção em dist/
```

Estrutura: `src/app/layout` (esqueleto responsivo), `src/app/core` (erros da API), `src/app/fichas` (modelo, catálogo, cliente da API e telas: lista, detalhe e formulário).
