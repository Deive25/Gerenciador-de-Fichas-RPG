-- V14: conta de demonstracao (incremento 2.6, spec do deploy §5.4). O POST /auth/demo cria um usuario temporario para o
-- visitante; a coluna marca essas contas para a limpeza (24 h) e para a API recusar que entrem em campanha de gente real.
-- criado_em ja existe desde a V10 (e e por ele que a limpeza sabe a idade da conta).
alter table usuario add column demo boolean not null default false;

-- A limpeza procura so as contas demo velhas: um indice parcial fica pequeno (quase nenhuma conta e demo).
create index ix_usuario_demo_criado_em on usuario (criado_em) where demo;
