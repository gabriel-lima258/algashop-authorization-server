-- QUAIS ESCOPOS CADA PAPEL PODE LEVAR, EM CADA CLIENT.
--
-- O escopo declarado no YAML do client e o TETO: o que aquele aplicativo pode PEDIR. Esta
-- tabela e o corte fino - dentro daquele teto, o que cada papel pode de fato RECEBER.
-- Por isso a mesma requisicao ao /oauth2/authorize, no mesmo client, devolve tokens
-- diferentes para MANAGER e OPERATOR.
--
-- Ex: ('MANAGER', 'algashop-admin-web', 'products:read')
--
-- A chave primaria e a linha inteira (papel + client + escopo) porque a linha NAO tem
-- identidade propria: ela e o proprio fato "este papel pode este escopo aqui". Um id
-- sintetico permitiria duplicatas do mesmo fato.
--
-- Ausencia de linha = negado. O default e fechado: escopo novo no client nao chega a papel
-- nenhum ate ser liberado aqui, explicitamente.
create table auth_user_type_client_scope (
   auth_user_type varchar(255) not null
       check (auth_user_type in ('MANAGER', 'OPERATOR', 'CUSTOMER')),
   client_id varchar(100) not null,
   scope varchar(100) not null,
   primary key (auth_user_type, client_id, scope)
);

-- A ordem das colunas espelha o WHERE do JdbcAuthUserClientScopesQueryService
-- (auth_user_type = ? AND client_id = ?). Indice composto so serve da esquerda para a
-- direita - invertido aqui, a consulta deixaria de usa-lo.
create index idx_auth_user_type_client_scope_lookup
   on auth_user_type_client_scope (auth_user_type, client_id);