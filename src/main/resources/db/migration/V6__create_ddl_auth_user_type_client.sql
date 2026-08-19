-- QUAIS CLIENTS CADA PAPEL PODE USAR.
--
-- A pergunta anterior a qualquer escopo: "esta pessoa pode sequer abrir esta aplicacao?".
-- Um CUSTOMER com senha valida e recusado no admin-web por esta tabela, ainda no
-- /oauth2/authorize, antes de a lista de escopos ser consultada e antes de existir codigo.
--
-- Repare que o CHECK repete os valores do enum AuthUserType. Sao duas fontes da mesma
-- verdade: acrescentar um papel novo no Java sem uma migration correspondente faz o INSERT
-- falhar em runtime. E redundancia proposital - o banco recusando um valor que a aplicacao
-- acha valido e ruim, mas menos ruim que o banco aceitar qualquer string como papel.
create table auth_user_type_client_allowed (
   auth_user_type varchar(255) not null check (auth_user_type in ('MANAGER', 'OPERATOR', 'CUSTOMER')),
   client_id varchar(100) not null,
   primary key (auth_user_type, client_id)
);

create index idx_auth_user_type_client_allowed_lookup
   on auth_user_type_client_allowed (auth_user_type);