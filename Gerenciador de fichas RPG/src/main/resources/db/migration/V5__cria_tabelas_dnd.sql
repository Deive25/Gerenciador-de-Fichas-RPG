-- V5: D&D 5e (2014) e D&D 2024 (incremento 2.1e, spec dos sistemas §3.3). Mesmo desenho do 2.1d (heranca JPA
-- JOINED): os dados comuns ficam em ficha, e os de D&D ficam em ficha_dnd, com o mesmo id da ficha.
-- Uma tabela serve as duas edicoes; a coluna edicao diz qual e.
-- A check de ficha.sistema ja aceita 'DND' (V4), entao a tabela ficha nao muda aqui.
-- O Flyway roda este arquivo numa transacao so (DDL transacional do PostgreSQL).
-- A V1 a V4 nao sao editadas (o Flyway compara o checksum de migracoes ja aplicadas).

-- 1) Dados de D&D: uma linha por ficha de D&D. Os limites (nivel de 1 a 20, atributos de 1 a 30, recursos >= 0)
--    tambem sao validados na API. Texto livre (classe, especie, antecedente) e opcional.
create table ficha_dnd (
    ficha_id      bigint       primary key references ficha (id) on delete cascade,
    edicao        varchar(5)   not null check (edicao in ('D2014', 'D2024')),
    classe        varchar(255),
    especie       varchar(255),
    antecedente   varchar(255),
    nivel         integer      not null check (nivel between 1 and 20),
    forca         integer      not null check (forca between 1 and 30),
    destreza      integer      not null check (destreza between 1 and 30),
    constituicao  integer      not null check (constituicao between 1 and 30),
    inteligencia  integer      not null check (inteligencia between 1 and 30),
    sabedoria     integer      not null check (sabedoria between 1 and 30),
    carisma       integer      not null check (carisma between 1 and 30),
    pv_atual      integer      check (pv_atual >= 0),
    pv_max        integer      check (pv_max >= 0),
    pv_temporario integer      check (pv_temporario >= 0),
    ca            integer      check (ca >= 0),
    inspiracao    boolean      not null default false,
    -- Salvaguardas em que a ficha e proficiente: uma coluna booleana por atributo.
    salva_forca        boolean not null default false,
    salva_destreza     boolean not null default false,
    salva_constituicao boolean not null default false,
    salva_inteligencia boolean not null default false,
    salva_sabedoria    boolean not null default false,
    salva_carisma      boolean not null default false
);

-- 2) Pericias de D&D: uma linha por pericia com proficiencia (sem linha = sem proficiencia). O nome da pericia
--    e o do enum PericiaDnd (gravado como texto), por isso nao ha check com a lista; o grau tem so dois valores.
create table ficha_dnd_pericia (
    ficha_id bigint      not null references ficha_dnd (ficha_id) on delete cascade,
    pericia  varchar(30) not null,
    grau     varchar(15) not null check (grau in ('PROFICIENTE', 'ESPECIALISTA')),
    primary key (ficha_id, pericia)
);
