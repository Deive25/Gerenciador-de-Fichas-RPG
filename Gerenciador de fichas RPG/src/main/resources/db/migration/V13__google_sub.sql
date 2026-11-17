-- V13: login com Google (incremento 2.5, spec §5). O "sub" do Google (ate 255 caracteres ASCII) e a identidade da conta
-- ligada; o e-mail pode mudar la. Sem CHECK exigindo senha ou google_sub: o usuario "legado" nao tem nenhum dos dois.
alter table usuario add column google_sub varchar(255);
alter table usuario add constraint uk_usuario_google_sub unique (google_sub);
