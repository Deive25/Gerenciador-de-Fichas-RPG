-- V2: move as 23 pericias das colunas atribute1..atribute23 de sheet_player para a tabela propria ficha_pericia.
-- O PostgreSQL tem DDL transacional: o Flyway roda este arquivo numa transacao so, entao se algum passo
-- falhar nada fica pela metade.
-- A V1 nao e editada (o Flyway compara o checksum de migracoes ja aplicadas).

-- 1) Uma linha por pericia preenchida. Pericia sem valor (null) nao gera linha.
--    O nome da pericia e o do enum Pericia (gravado como texto); o enum e a fonte da verdade, por isso
--    nao ha check constraint com a lista.
create table ficha_pericia (
    ficha_id bigint      not null references sheet_player (id) on delete cascade,
    pericia  varchar(40) not null,
    valor    integer     not null,
    primary key (ficha_id, pericia)
);

-- 2) Copia os dados existentes, uma pericia por vez (atributeN -> constante do enum).
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ACROBACIA', atribute1 from sheet_player where atribute1 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ATLETISMO', atribute2 from sheet_player where atribute2 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ADESTRAR_ANIMAIS', atribute3 from sheet_player where atribute3 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ATUACAO_LABIA', atribute4 from sheet_player where atribute4 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'CHARME', atribute5 from sheet_player where atribute5 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ESQUIVA', atribute6 from sheet_player where atribute6 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'FURTIVIDADE', atribute7 from sheet_player where atribute7 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'LUTA_CORPO_A_CORPO', atribute8 from sheet_player where atribute8 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'LUTA_ARMA_BRANCA', atribute9 from sheet_player where atribute9 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'INTIMIDACAO', atribute10 from sheet_player where atribute10 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'INTUICAO', atribute11 from sheet_player where atribute11 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'INVESTIGACAO', atribute12 from sheet_player where atribute12 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ARMAS_DE_FOGO_LEVES', atribute13 from sheet_player where atribute13 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ARMAS_DE_FOGO_MEDIAS', atribute14 from sheet_player where atribute14 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ARMAS_DE_FOGO_PESADAS', atribute15 from sheet_player where atribute15 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'PERCEPCAO', atribute16 from sheet_player where atribute16 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'USAR_BIBLIOTECAS', atribute17 from sheet_player where atribute17 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'HISTORIA', atribute18 from sheet_player where atribute18 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'CURA', atribute19 from sheet_player where atribute19 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'MALANDRAGEM', atribute20 from sheet_player where atribute20 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'OCULTISMO', atribute21 from sheet_player where atribute21 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'PSICOLOGIA', atribute22 from sheet_player where atribute22 is not null;
insert into ficha_pericia (ficha_id, pericia, valor) select id, 'ARREMESSAR', atribute23 from sheet_player where atribute23 is not null;

-- 3) Remove as colunas antigas (so depois da copia).
alter table sheet_player
    drop column atribute1,
    drop column atribute2,
    drop column atribute3,
    drop column atribute4,
    drop column atribute5,
    drop column atribute6,
    drop column atribute7,
    drop column atribute8,
    drop column atribute9,
    drop column atribute10,
    drop column atribute11,
    drop column atribute12,
    drop column atribute13,
    drop column atribute14,
    drop column atribute15,
    drop column atribute16,
    drop column atribute17,
    drop column atribute18,
    drop column atribute19,
    drop column atribute20,
    drop column atribute21,
    drop column atribute22,
    drop column atribute23;
