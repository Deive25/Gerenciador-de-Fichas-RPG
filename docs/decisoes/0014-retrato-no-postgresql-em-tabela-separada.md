# 0014. Retrato do personagem no PostgreSQL, numa tabela separada

Status: aceita · Incremento 2.1c · 2026-11-04

## Contexto
O Arthur pediu que o jogador possa escolher a imagem do personagem (upload próprio). O deploy previsto é só gratuito (Render + Neon, 0,5 GB), e o projeto ainda não tem login. As listagens do painel trazem todas as fichas, então qualquer coluna pesada na tabela da ficha seria lida em toda listagem.

## Decisão
- **Onde fica a imagem:** a tabela `retrato_ficha (ficha_id PK/FK on delete cascade, conteudo bytea, atualizado_em timestamptz)`, criada pela migração V3. Um retrato por ficha; enviar de novo substitui.
- **A listagem não carrega os bytes:** a ficha só expõe `retratoVersao` (o `atualizado_em` em milissegundos), lido por um `@Formula` com subconsulta no mesmo SELECT. A listagem continua com uma consulta só (sem N+1), e o teste que prova isso não mudou.
- **O navegador manda pouco:** ele recorta (4:5) e reduz para 400×500 em JPEG antes de enviar (≈ 10–60 KB).
- **O servidor não confia no navegador:** aceita só `image/jpeg`, no máximo 200 KB (leitura limitada do corpo), com os bytes iniciais `FF D8 FF`, que decodifique com `ImageIO` e tenha no máximo 1000 px por lado. SVG nunca passa. O `GET` responde com `nosniff`.
- **Cache sem imagem velha:** a URL que o front usa leva a versão (`/retrato?v=…`), então a resposta pode ser `immutable`. A versão vem de um `Clock` injetável e nunca se repete (se o relógio não andou, versão anterior + 1 ms).
- **Exibição:** o front baixa como `blob` pelo `HttpClient` e cria uma URL local, revogada quando a versão muda. Com o login (2.2), um `<img src>` direto não mandaria o cabeçalho `Authorization`.

## Alternativas consideradas
- **Armazenamento de objetos (S3, Cloudflare R2):** é o padrão para arquivos em produção (CDN, sem peso no banco), mas acrescenta um serviço, credenciais e, em alguns planos, cadastro de cartão. Reavaliar no 2.6 (deploy), se o volume crescer.
- **Coluna `bytea` na própria `sheet_player`:** mais simples, mas toda listagem leria (ou precisaria excluir com cuidado) os bytes das imagens.
- **Guardar o arquivo em disco no servidor:** o Render gratuito não tem disco persistente; o arquivo some a cada deploy.
- **Base64 dentro do JSON da ficha:** incha cada resposta em ~33% e mistura a imagem com os dados.

## Consequências
- Cabem milhares de retratos nos 0,5 GB do Neon gratuito, e excluir a ficha apaga o retrato junto (cascata no banco).
- O backup do banco já inclui as imagens; não há um segundo lugar para sincronizar.
- **Pior:** imagens no banco aumentam o tamanho dos backups e não passam por CDN. Para um portfólio com poucas fichas é aceitável; com muitas, o armazenamento de objetos volta à mesa.
- **Pior:** a `@Formula` é SQL específico do PostgreSQL (`extract('epoch' …)`), o que já é o caso do projeto inteiro desde a V1 com Flyway.
