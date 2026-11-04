-- V3: retrato do personagem (incremento 2.1c, spec do redesign §4.4).
-- Tabela separada de sheet_player para que a listagem de fichas nunca leia bytes de imagem.
-- Uma linha por ficha (ficha_id é a chave); enviar outro retrato substitui a linha.
-- Excluir a ficha apaga o retrato junto (on delete cascade).
-- atualizado_em vira a "versão" do retrato na API (milissegundos), usada para o cache do navegador.
create table retrato_ficha (
    ficha_id      bigint      primary key references sheet_player (id) on delete cascade,
    conteudo      bytea       not null,
    atualizado_em timestamptz not null
);
