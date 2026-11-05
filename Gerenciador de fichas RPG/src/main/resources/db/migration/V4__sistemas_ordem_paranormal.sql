-- V4: varios sistemas de RPG (incremento 2.1d, spec dos sistemas §3.1): sheet_player vira ficha (dados comuns
-- + sistema), e os dados de Ordem Paranormal ficam em ficha_ordem (heranca JPA JOINED: uma tabela por sistema).
-- As fichas d100 que ja existem viram fichas de Ordem. Os ids nao mudam, porque o retrato (retrato_ficha) e as
-- URLs /fichas/{id} dependem deles.
-- O PostgreSQL tem DDL transacional: o Flyway roda este arquivo numa transacao so, entao se algum passo
-- falhar nada fica pela metade.
-- A V1, a V2 e a V3 nao sao editadas (o Flyway compara o checksum de migracoes ja aplicadas).

-- 1) A tabela passa a se chamar ficha, e as colunas comuns ganham nomes em portugues.
--    Os apontamentos de retrato_ficha acompanham a tabela renomeada (o PostgreSQL guarda a referencia pelo objeto).
alter table sheet_player rename to ficha;
alter table ficha rename column namej  to jogador;
alter table ficha rename column namec  to personagem;
alter table ficha rename column gender to genero;
alter table ficha rename column age    to idade;

-- 2) Coluna que diz de qual sistema e a ficha (o discriminador do JPA). Toda ficha existente e de Ordem
--    Paranormal; o default so serve para preencher as linhas atuais e sai em seguida, assim uma ficha nova
--    sempre precisa dizer o sistema.
alter table ficha add column sistema varchar(30) not null default 'ORDEM_PARANORMAL';
alter table ficha alter column sistema drop default;
alter table ficha add constraint ficha_sistema_check check (sistema in ('ORDEM_PARANORMAL', 'DND'));

-- 3) Dados de Ordem Paranormal: uma linha por ficha de Ordem, com o mesmo id da ficha.
--    Os limites (NEX de 5 a 99, atributos de 0 a 5, recursos >= 0) tambem sao validados na API.
create table ficha_ordem (
    ficha_id  bigint       primary key references ficha (id) on delete cascade,
    classe    varchar(20)  check (classe in ('COMBATENTE', 'ESPECIALISTA', 'OCULTISTA')),
    origem    varchar(255),
    nex       integer      not null check (nex between 5 and 99),
    agilidade integer      not null check (agilidade between 0 and 5),
    forca     integer      not null check (forca between 0 and 5),
    intelecto integer      not null check (intelecto between 0 and 5),
    presenca  integer      not null check (presenca between 0 and 5),
    vigor     integer      not null check (vigor between 0 and 5),
    pv_atual  integer      check (pv_atual >= 0),
    pv_max    integer      check (pv_max >= 0),
    san_atual integer      check (san_atual >= 0),
    san_max   integer      check (san_max >= 0),
    pe_atual  integer      check (pe_atual >= 0),
    pe_max    integer      check (pe_max >= 0)
);

-- 4) Perícias de Ordem: uma linha por perícia treinada (sem linha = destreinada). O nome da perícia e o do
--    enum PericiaOrdem (gravado como texto), por isso nao ha check com a lista; o grau tem so tres valores.
create table ficha_ordem_pericia (
    ficha_id bigint      not null references ficha_ordem (ficha_id) on delete cascade,
    pericia  varchar(30) not null,
    grau     varchar(10) not null check (grau in ('TREINADO', 'VETERANO', 'EXPERT')),
    primary key (ficha_id, pericia)
);

-- 5) Converte as fichas existentes: NEX 5 e atributos 1 (valores validos e neutros). Vida e sanidade antigas
--    viram PV e SAN; PE fica vazio (o d100 nao tinha). Vida/sanidade nulas continuam nulas.
insert into ficha_ordem (ficha_id, nex, agilidade, forca, intelecto, presenca, vigor, pv_atual, pv_max, san_atual, san_max)
select id, 5, 1, 1, 1, 1, 1, lifea, lifem, sana, sanm from ficha;

-- 6) Remove o que era so do d100 (so depois da copia). As perícias d100 nao tem equivalente em Ordem,
--    entao ficha_pericia sai junto, sem migrar valores.
drop table ficha_pericia;
alter table ficha
    drop column lifea,
    drop column lifem,
    drop column sana,
    drop column sanm,
    drop column str,
    drop column dex,
    drop column cons,
    drop column "int",
    drop column car,
    drop column sab,
    drop column tam,
    drop column sor,
    drop column pod;
